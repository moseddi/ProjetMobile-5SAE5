package com.hangout.repository;

import com.hangout.config.MongoConfig;
import com.hangout.model.HangOut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository (data layer) tests against a real, embedded MongoDB instance, so
 * the derived query methods are validated with actual Mongo queries — not mocks.
 * MongoConfig (@EnableMongoAuditing) is imported explicitly: @DataMongoTest does not
 * pick it up on its own, and createdAt/updatedAt rely on it being active.
 */
@DataMongoTest
@Import(MongoConfig.class)
@ActiveProfiles("test")
@DisplayName("HangOutRepository - data layer")
class HangOutRepositoryTest {

    @Autowired
    private HangOutRepository hangOutRepository;

    private String creatorId;
    private String participantId;

    @BeforeEach
    void setUp() {
        hangOutRepository.deleteAll();
        creatorId = "creator-1";
        participantId = "joiner-1";

        hangOutRepository.save(hangOut("Italian Night", creatorId, HangOut.HangOutStatus.OPEN, 3, null));
        hangOutRepository.save(hangOut("Sushi Night", "creator-2", HangOut.HangOutStatus.FULL, 1, participantId));
        hangOutRepository.save(hangOut("Cancelled Brunch", "creator-3", HangOut.HangOutStatus.CANCELLED, 7, null));
    }

    @Test
    @DisplayName("findAllByOrderByPlannedDateTimeAsc returns hangouts sorted by date")
    void findAllOrderedByDate() {
        List<HangOut> result = hangOutRepository.findAllByOrderByPlannedDateTimeAsc();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(HangOut::getTitle)
            .containsExactly("Sushi Night", "Italian Night", "Cancelled Brunch");
    }

    @Test
    @DisplayName("findByStatusInOrderByPlannedDateTimeAsc returns only OPEN/FULL, sorted")
    void findByStatusInOrderedByDate() {
        List<HangOut> result = hangOutRepository.findByStatusInOrderByPlannedDateTimeAsc(
            List.of(HangOut.HangOutStatus.OPEN, HangOut.HangOutStatus.FULL));

        assertThat(result).extracting(HangOut::getStatus)
            .containsOnly(HangOut.HangOutStatus.OPEN, HangOut.HangOutStatus.FULL);
        assertThat(result).extracting(HangOut::getTitle)
            .containsExactly("Sushi Night", "Italian Night");
    }

    @Test
    @DisplayName("findByCreatorId returns only the hangouts of that creator")
    void findByCreatorId() {
        List<HangOut> result = hangOutRepository.findByCreatorId(creatorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Italian Night");
    }

    @Test
    @DisplayName("findByParticipantsUserId finds hangouts a user joined")
    void findByParticipantsUserId() {
        List<HangOut> result = hangOutRepository.findByParticipantsUserId(participantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Sushi Night");
    }

    @Test
    @DisplayName("saving assigns an id and an audit createdAt timestamp")
    void saveAssignsIdAndCreatedAt() {
        HangOut saved = hangOutRepository.save(
            hangOut("Audit Check", creatorId, HangOut.HangOutStatus.OPEN, 9, null));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(hangOutRepository.findById(saved.getId())).isPresent();
    }

    private HangOut hangOut(String title, String creator, HangOut.HangOutStatus status,
                            int daysFromNow, String joinedUserId) {
        HangOut hangOut = new HangOut();
        hangOut.setTitle(title);
        hangOut.setCreatorId(creator);
        hangOut.setRestaurantId("restaurant-1");
        hangOut.setDescription("description");
        hangOut.setMaxParticipants(6);
        hangOut.setStatus(status);
        hangOut.setPlannedDateTime(LocalDateTime.now().plusDays(daysFromNow));
        if (joinedUserId != null) {
            HangOut.Participant participant = new HangOut.Participant();
            participant.setUserId(joinedUserId);
            participant.setStatus(HangOut.ParticipantStatus.ACCEPTED);
            hangOut.setParticipants(List.of(participant));
        }
        return hangOut;
    }
}
