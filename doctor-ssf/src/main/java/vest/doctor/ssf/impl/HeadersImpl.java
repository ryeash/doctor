package vest.doctor.ssf.impl;

import vest.doctor.ssf.Headers;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class HeadersImpl implements Headers {
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public void add(String headerName, String headerValue) {
        headers.computeIfAbsent(headerName, v -> new LinkedList<>()).add(headerValue);
    }

    @Override
    public void set(String headerName, String headerValue) {
        remove(headerName);
        add(headerName, headerValue);
    }

    @Override
    public void remove(String headerName) {
        headers.remove(headerName);
    }

    @Override
    public String get(String headerName) {
        return Optional.ofNullable(headers.get(headerName))
                .filter(l -> !l.isEmpty())
                .map(l -> l.get(0))
                .orElse(null);
    }

    @Override
    public List<String> getAll(String headerName) {
        return Collections.unmodifiableList(headers.getOrDefault(headerName, List.of()));
    }

    @Override
    public Collection<String> headerNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return headers.entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), String.join(",", entry.getValue()))).iterator();
    }
}
