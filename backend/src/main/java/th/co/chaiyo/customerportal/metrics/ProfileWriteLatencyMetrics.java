package th.co.chaiyo.customerportal.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;

/**
 * R-10/NFR-1: the portal had no server-side latency metrics before this.
 * The total PATCH duration and the outbound Customer360 write duration are
 * recorded as separate timers rather than one number, because the 2s budget
 * was set with no measured Customer360 latency behind it (Q-5) — if the
 * downstream call alone consumes the budget, that has to be visible on its
 * own, not folded into the total.
 */
@Component
public class ProfileWriteLatencyMetrics {

    static final String PATCH_TIMER_NAME = "customerportal.profile.patch";
    static final String CUSTOMER360_WRITE_TIMER_NAME = "customerportal.profile.patch.customer360.write";

    private final Timer patchTimer;
    private final Timer customer360WriteTimer;

    public ProfileWriteLatencyMetrics(MeterRegistry registry) {
        this.patchTimer = Timer.builder(PATCH_TIMER_NAME)
                .description("Server-side latency of PATCH /v1/customers/{id}/profile")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        this.customer360WriteTimer = Timer.builder(CUSTOMER360_WRITE_TIMER_NAME)
                .description("Latency of the outbound Customer360 profile write within a PATCH")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public <T> Mono<T> timePatch(Mono<T> source) {
        return time(source, patchTimer);
    }

    public <T> Mono<T> timeCustomer360Write(Mono<T> source) {
        return time(source, customer360WriteTimer);
    }

    private <T> Mono<T> time(Mono<T> source, Timer timer) {
        return Mono.defer(() -> {
            long startNanos = System.nanoTime();
            return source.doFinally(signal -> timer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS));
        });
    }
}
