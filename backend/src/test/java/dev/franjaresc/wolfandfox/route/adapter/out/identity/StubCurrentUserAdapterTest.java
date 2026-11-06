package dev.franjaresc.wolfandfox.route.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.franjaresc.wolfandfox.route.domain.UserId;
import org.junit.jupiter.api.Test;

class StubCurrentUserAdapterTest {

    @Test
    void returnsTheFictionalDemoUser() {
        assertThat(new StubCurrentUserAdapter().currentUser()).isEqualTo(new UserId("demo.supervisor"));
    }
}
