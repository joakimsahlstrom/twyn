open module twyn {
    requires guava;
    requires java.compiler;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires compilation.toolbox;
    requires java.logging;

    exports se.jsa.twyn;
}