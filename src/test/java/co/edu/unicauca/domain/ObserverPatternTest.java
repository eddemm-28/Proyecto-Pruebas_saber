package co.edu.unicauca.domain;

import co.edu.unicauca.access.QuestionImplRepository;
import co.edu.unicauca.infra.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObserverPatternTest {

    private QuestionService service;
    private TestObserver observer1;
    private TestObserver observer2;

    private static class TestObserver implements Observer {
        int updateCount = 0;
        Object lastUpdatedSubject = null;

        @Override
        public void update(Object obj) {
            updateCount++;
            lastUpdatedSubject = obj;
        }
    }

    @BeforeEach
    void setUp() {
        service = new QuestionService(new QuestionImplRepository());
        observer1 = new TestObserver();
        observer2 = new TestObserver();
    }

    @Test
    @DisplayName("Debe notificar a los observadores registrados cuando cambie el estado de una pregunta")
    void testObserverNotificationOnStateUpdate() {
        service.addObserver(observer1);
        service.addObserver(observer2);

        assertEquals(0, observer1.updateCount);
        assertEquals(0, observer2.updateCount);

        service.updateQuestionState("P-001", Question.STATE_ELIMINADA);

        assertEquals(1, observer1.updateCount);
        assertEquals(1, observer2.updateCount);
        assertEquals(service, observer1.lastUpdatedSubject);
    }

    @Test
    @DisplayName("No debe notificar a observadores removidos")
    void testRemoveObserver() {
        service.addObserver(observer1);
        service.addObserver(observer2);

        service.removeObserver(observer2);

        service.updateQuestionState("P-002", Question.STATE_PENDIENTE_REVISION);

        assertEquals(1, observer1.updateCount);
        assertEquals(0, observer2.updateCount);
    }
}
