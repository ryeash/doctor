package vest.doctor.codegen;

import java.util.stream.Collectors;

public class CodeSnippet extends AbstractCodeBuilder<CodeSnippet> {
    @Override
    public String toString() {
        return allLines().collect(Collectors.joining("\n"));
    }
}
