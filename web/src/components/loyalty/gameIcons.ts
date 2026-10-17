export const iconForGame = (gameId: string): string => {
  switch (gameId) {
    case 'quiz': return '🧠';
    case 'memory': return '🃏';
    case 'scratch': return '🎫';
    case 'wheel': return '🎡';
    default: return '🎮';
  }
};
