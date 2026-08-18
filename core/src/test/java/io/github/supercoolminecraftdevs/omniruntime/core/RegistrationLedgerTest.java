package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationLedgerTest {

    private static final class Recorded implements Registration {
        private final String description;
        private final List<String> log;
        private final RuntimeException failure;
        private int revokeCount;

        Recorded(String description, List<String> log, RuntimeException failure) {
            this.description = description;
            this.log = log;
            this.failure = failure;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public void revoke() {
            revokeCount++;
            log.add(description);
            if (failure != null) {
                throw failure;
            }
        }
    }

    @Test
    void revokesEverythingItWasGiven() {
        List<String> revoked = new ArrayList<>();
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.record(new Recorded("event subscription", revoked, null));
        ledger.record(new Recorded("scheduled task", revoked, null));

        assertTrue(ledger.revokeAll().isEmpty());
        assertEquals(List.of("scheduled task", "event subscription"), revoked);
        assertEquals(0, ledger.size());
    }

    @Test
    void revokesInReverseOrder() {
        List<String> revoked = new ArrayList<>();
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.record(new Recorded("first", revoked, null));
        ledger.record(new Recorded("second", revoked, null));
        ledger.record(new Recorded("third", revoked, null));

        ledger.revokeAll();

        assertEquals(List.of("third", "second", "first"), revoked);
    }

    @Test
    void oneFailureDoesNotStopTheRest() {
        List<String> revoked = new ArrayList<>();
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.record(new Recorded("survives", revoked, null));
        ledger.record(new Recorded("throws", revoked, new IllegalStateException("no")));
        ledger.record(new Recorded("also survives", revoked, null));

        List<RegistrationLedger.RevokeFailure> failures = ledger.revokeAll();

        assertEquals(List.of("also survives", "throws", "survives"), revoked);
        assertEquals(1, failures.size());
        assertEquals("throws", failures.get(0).description());
    }

    @Test
    void revokesEachRegistrationOnce() {
        List<String> revoked = new ArrayList<>();
        Recorded once = new Recorded("once", revoked, null);
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.record(once);

        ledger.revokeAll();
        ledger.revokeAll();

        assertEquals(1, once.revokeCount);
    }

    @Test
    void refusesNewRegistrationsAfterTeardown() {
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.revokeAll();

        assertTrue(ledger.isClosed());
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> ledger.record(new Recorded("too late", new ArrayList<>(), null)));
        assertTrue(thrown.getMessage().contains("torn down"), thrown.getMessage());
    }

    @Test
    void startsOpenAndEmpty() {
        RegistrationLedger ledger = new RegistrationLedger();

        assertEquals(0, ledger.size());
        assertFalse(ledger.isClosed());
    }
}
