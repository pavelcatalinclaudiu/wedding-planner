package ro.eternelle.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Converter
public class UUIDListConverter implements AttributeConverter<List<UUID>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<UUID>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(uuids);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<UUID> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
