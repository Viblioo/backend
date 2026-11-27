package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j.node;

import com.revibe.backend.domain.model.EventVisibility;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.types.GeographicPoint2d;

@Node("Event")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventNode {

    @Id
    private String id; // el UUID de PostgreSQL; no se genera aquí

    private EventVisibility visibility; // se guarda como "PUBLIC"

    private Instant startsAt; // se guarda como DateTime

    private GeographicPoint2d location; // se guarda como Point (WGS-84)

    public static GeographicPoint2d toLocation(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return null; // en tu tabla lat/lon pueden ser null
        }
        return new GeographicPoint2d(lat.doubleValue(), lon.doubleValue()); // (latitud, longitud)
    }
}
