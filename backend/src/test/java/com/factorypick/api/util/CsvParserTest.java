package com.factorypick.api.util;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class CsvParserTest {
    @Test
    void quotedCommaAndEscapedQuoteAreParsed() throws Exception {
        String csv = "name,address,note\n\"공장, 1\",서울,\"좋은 \"\"제품\"\"\"\n";
        var rows = CsvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("name")).isEqualTo("공장, 1");
        assertThat(rows.getFirst().get("note")).isEqualTo("좋은 \"제품\"");
    }
}
