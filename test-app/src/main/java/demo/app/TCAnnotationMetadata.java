package demo.app;

import jakarta.inject.Singleton;

@Singleton
@Everything(string = "a",
        strings = {"b", "c"},
        byteVal = (byte) 1,
        byteArr = {(byte) 2, (byte) 3},
        shortVal = (short) 4,
        shortArr = {(short) 5, (short) 6},
        intVal = 7,
        intArr = {8, 9},
        longVal = 10L,
        longArr = {11, 12},
        floatVal = 13.1F,
        floatArr = {14.2F, 15.3F},
        doubleVal = 16.4D,
        doubleArr = {17.5D, 18.6D},
        boolVal = true,
        boolArr = {true, false},
        annotationVal = @CustomQualifier(name = "nested"),
        annotationArr = {@CustomQualifier(name = "two", color = CustomQualifier.Color.BLACK), @CustomQualifier(name = "three")},
        enumeration = Everything.Letter.A,
        enumerations = {Everything.Letter.B, Everything.Letter.C},
        classVal = String.class,
        classArr = {Integer.class, Long.class})
public class TCAnnotationMetadata {
}

