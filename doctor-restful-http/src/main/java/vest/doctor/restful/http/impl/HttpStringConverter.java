package vest.doctor.restful.http.impl;

import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.StringConversionGenerator;

import javax.lang.model.type.TypeMirror;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;

public class HttpStringConverter implements StringConversionGenerator {

    @Override
    public String converterFunction(AnnotationProcessorContext context, TypeMirror targetType) {
        if (ProcessorUtils.isCompatibleWith(context, targetType, Locale.class)) {
            return "str -> java.util.Locale.forLanguageTag(str.contains(\";\") ? str.substring(0, str.indexOf(';')) : str)";
        } else if (ProcessorUtils.isCompatibleWith(context, targetType, ZonedDateTime.class)) {
            return "str -> java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(str, java.time.ZonedDateTime::from)";
        } else if (ProcessorUtils.isCompatibleWith(context, targetType, LocalDateTime.class)) {
            return "str -> java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(str, java.time.LocalDateTime::from)";
        } else if (ProcessorUtils.isCompatibleWith(context, targetType, Instant.class)) {
            return "str -> java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(str, java.time.Instant::from)";
        }
        return null;
    }
}
