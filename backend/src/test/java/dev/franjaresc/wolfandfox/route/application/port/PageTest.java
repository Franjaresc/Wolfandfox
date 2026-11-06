package dev.franjaresc.wolfandfox.route.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageTest {

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void rejectsPageQueryOutOfBounds(int page, int size) {
        assertThatThrownBy(() -> new PageQuery(page, size)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,100", "7,20"})
    void acceptsPageQueryWithinBounds(int page, int size) {
        assertThat(new PageQuery(page, size).size()).isEqualTo(size);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "20,1", "21,2", "40,2", "41,3"})
    void derivesTotalPagesRoundingUp(long totalElements, int expectedPages) {
        var result = new PageResult<>(List.of(), new PageQuery(0, 20), totalElements);

        assertThat(result.totalPages()).isEqualTo(expectedPages);
    }

    @Test
    void itemsCannotChangeAfterCreation() {
        var source = new ArrayList<>(List.of("a"));
        var result = new PageResult<>(source, new PageQuery(0, 20), 1);

        source.add("b");

        assertThat(result.items()).containsExactly("a");
    }
}
