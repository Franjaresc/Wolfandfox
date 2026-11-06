package dev.franjaresc.wolfandfox.route.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PackageCountsTest {

    @Test
    void derivesTotalFromTheThreeCounts() {
        assertThat(new PackageCounts(42, 3, 1).total()).isEqualTo(46);
    }

    @Test
    void acceptsZeroCounts() {
        assertThat(new PackageCounts(0, 0, 0).total()).isZero();
    }

    @Test
    void totalDoesNotOverflowWhenCountsExceedIntRange() {
        var counts = new PackageCounts(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThat(counts.total()).isEqualTo(3L * Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @CsvSource({"-1,0,0", "0,-1,0", "0,0,-1"})
    void rejectsNegativeCounts(int delivered, int failed, int returned) {
        assertThatThrownBy(() -> new PackageCounts(delivered, failed, returned))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("must not be negative");
    }
}
