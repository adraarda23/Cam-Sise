package ardaaydinkilinc.Cam_Sise.core.service.event;

import ardaaydinkilinc.Cam_Sise.core.domain.event.*;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoreEventHandler Tests")
class CoreEventHandlerTest {

    @InjectMocks
    private CoreEventHandler handler;

    @Test
    @DisplayName("PoolOperatorRegistered eventini handle etmeli")
    void handlesPoolOperatorRegistered() {
        var event = new PoolOperatorRegistered("Test A.Ş.", "1234567890", LocalDateTime.now());
        assertThatCode(() -> handler.handlePoolOperatorRegistered(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PoolOperatorActivated eventini handle etmeli")
    void handlesPoolOperatorActivated() {
        var event = new PoolOperatorActivated(1L, "Test A.Ş.", LocalDateTime.now());
        assertThatCode(() -> handler.handlePoolOperatorActivated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PoolOperatorDeactivated eventini handle etmeli")
    void handlesPoolOperatorDeactivated() {
        var event = new PoolOperatorDeactivated(1L, "Test A.Ş.", LocalDateTime.now());
        assertThatCode(() -> handler.handlePoolOperatorDeactivated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FillerRegistered eventini handle etmeli")
    void handlesFillerRegistered() {
        var event = new FillerRegistered(1L, "Test Dolumcu",
                new GeoCoordinates(41.0, 29.0), LocalDateTime.now());
        assertThatCode(() -> handler.handleFillerRegistered(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FillerActivated eventini handle etmeli")
    void handlesFillerActivated() {
        var event = new FillerActivated(1L, 1L, "Test Dolumcu", LocalDateTime.now());
        assertThatCode(() -> handler.handleFillerActivated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FillerDeactivated eventini handle etmeli")
    void handlesFillerDeactivated() {
        var event = new FillerDeactivated(1L, 1L, "Test Dolumcu", LocalDateTime.now());
        assertThatCode(() -> handler.handleFillerDeactivated(event)).doesNotThrowAnyException();
    }
}
