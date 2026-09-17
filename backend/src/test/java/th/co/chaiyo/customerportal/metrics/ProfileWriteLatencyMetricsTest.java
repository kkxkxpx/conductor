package th.co.chaiyo.customerportal.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProfileWriteLatencyMetricsTest {

    private SimpleMeterRegistry registry;
    private ProfileWriteLatencyMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ProfileWriteLatencyMetrics(registry);
    }

    private Timer patchTimer() {
        return registry.find(ProfileWriteLatencyMetrics.PATCH_TIMER_NAME).timer();
    }

    private Timer customer360WriteTimer() {
        return registry.find(ProfileWriteLatencyMetrics.CUSTOMER360_WRITE_TIMER_NAME).timer();
    }

    @Test
    void timePatchRecordsOneSampleOnThePatchTimerWhenTheSourceCompletes() {
        StepVerifier.create(metrics.timePatch(Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();

        assertThat(patchTimer().count()).isEqualTo(1);
    }

    @Test
    void timePatchPropagatesTheSourceValueUnchanged() {
        StepVerifier.create(metrics.timePatch(Mono.just("profile-body")))
                .expectNext("profile-body")
                .verifyComplete();
    }

    @Test
    void timePatchRecordsASampleEvenWhenTheSourceErrors() {
        RuntimeException boom = new RuntimeException("boom");

        StepVerifier.create(metrics.timePatch(Mono.error(boom)))
                .expectErrorMatches(thrown -> thrown == boom)
                .verify();

        assertThat(patchTimer().count()).isEqualTo(1);
    }

    @Test
    void timeCustomer360WriteRecordsOnTheCustomer360TimerNotThePatchTimer() {
        StepVerifier.create(metrics.timeCustomer360Write(Mono.just("updated")))
                .expectNext("updated")
                .verifyComplete();

        assertThat(customer360WriteTimer().count()).isEqualTo(1);
        assertThat(patchTimer().count()).isEqualTo(0);
    }

    @Test
    void timePatchAndTimeCustomer360WriteRecordOnIndependentTimersWhenNested() {
        StepVerifier.create(metrics.timePatch(metrics.timeCustomer360Write(Mono.just("updated"))))
                .expectNext("updated")
                .verifyComplete();

        assertThat(patchTimer().count()).isEqualTo(1);
        assertThat(customer360WriteTimer().count()).isEqualTo(1);
    }

    @Test
    void patchTimerPublishesThe50th95thAnd99thPercentiles() {
        metrics.timePatch(Mono.just("ok")).block();

        ValueAtPercentile[] percentiles = patchTimer().takeSnapshot().percentileValues();

        assertThat(percentiles).extracting(ValueAtPercentile::percentile)
                .containsExactlyInAnyOrder(0.5, 0.95, 0.99);
    }

    @Test
    void customer360WriteTimerPublishesThe50th95thAnd99thPercentiles() {
        metrics.timeCustomer360Write(Mono.just("updated")).block();

        ValueAtPercentile[] percentiles = customer360WriteTimer().takeSnapshot().percentileValues();

        assertThat(percentiles).extracting(ValueAtPercentile::percentile)
                .containsExactlyInAnyOrder(0.5, 0.95, 0.99);
    }
}
