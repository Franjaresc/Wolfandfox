package dev.franjaresc.wolfandfox.route.adapter.out.identity;

import dev.franjaresc.wolfandfox.route.application.port.out.CurrentUserPort;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import org.springframework.stereotype.Component;

/** Returns a fixed fictional user until authentication exists. See docs/backlog.md. */
@Component
class StubCurrentUserAdapter implements CurrentUserPort {

    private static final UserId DEMO_USER = new UserId("demo.supervisor");

    @Override
    public UserId currentUser() {
        return DEMO_USER;
    }
}
