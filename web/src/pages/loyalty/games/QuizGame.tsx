import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { gameService } from '../../../services/loyalty/gameService';
import { QuizQuestion, GameResultResponse } from '../../../types';
import LoyaltyNav from '../../../components/loyalty/LoyaltyNav';
import '../LoyaltyPage.css';
import './GamePlay.css';

const QuizGame = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<GameResultResponse | null>(null);

  useEffect(() => {
    if (!user?.id) return;
    gameService.getTodaysQuiz(user.id)
      .then(setQuestions)
      .catch(() => navigate('/loyalty/games'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const selectAnswer = (questionId: string, index: number) => {
    setAnswers((prev) => ({ ...prev, [questionId]: index }));
  };

  const next = () => {
    if (current < questions.length - 1) {
      setCurrent((c) => c + 1);
    } else {
      submit();
    }
  };

  const submit = async () => {
    if (!user?.id) return;
    setSubmitting(true);
    try {
      const payload = {
        answers: questions.map((q) => ({ questionId: q.id, selectedIndex: answers[q.id] ?? -1 })),
      };
      const res = await gameService.submitQuiz(user.id, payload);
      setResult(res);
    } catch {
      /* handled by apiClient */
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="loading"><div className="spinner"></div></div>
      </div>
    );
  }

  if (result) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="container game-result-page">
          <h1>{result.message}</h1>
          <p className="game-result-score">{result.correctAnswers} / {result.totalQuestions} correct answers</p>
          <p className="game-result-points">+{result.pointsEarned} points — new balance: {result.newBalance}</p>

          <div className="quiz-explanations">
            {result.explanations?.map((exp) => {
              const question = questions.find((q) => q.id === exp.questionId);
              return (
                <div key={exp.questionId} className={`quiz-explanation ${exp.correct ? 'correct' : 'incorrect'}`}>
                  <p className="quiz-explanation-question">{question?.question}</p>
                  <p className="quiz-explanation-answer">
                    {exp.correct ? '✓ Correct' : `✗ Correct answer: ${question?.options[exp.correctAnswerIndex]}`}
                  </p>
                  <p className="quiz-explanation-text">{exp.explanation}</p>
                </div>
              );
            })}
          </div>

          <button className="btn btn-primary" onClick={() => navigate('/loyalty/games')}>Back to games</button>
        </div>
      </div>
    );
  }

  const question = questions[current];
  if (!question) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="container game-result-page">
          <h2>No quiz available right now.</h2>
          <button className="btn btn-outline" onClick={() => navigate('/loyalty/games')}>Back to games</button>
        </div>
      </div>
    );
  }

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container quiz-page">
        <div className="quiz-progress">Question {current + 1} / {questions.length}</div>
        <h2 className="quiz-question">{question.question}</h2>
        <div className="quiz-options">
          {question.options.map((opt, i) => (
            <button
              key={i}
              className={`quiz-option ${answers[question.id] === i ? 'selected' : ''}`}
              onClick={() => selectAnswer(question.id, i)}
            >
              {opt}
            </button>
          ))}
        </div>
        <button
          className="btn btn-primary btn-block"
          disabled={answers[question.id] === undefined || submitting}
          onClick={next}
        >
          {submitting ? 'Submitting…' : current < questions.length - 1 ? 'Next question' : 'Submit quiz'}
        </button>
      </div>
    </div>
  );
};

export default QuizGame;
