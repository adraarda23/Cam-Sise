package ardaaydinkilinc.Cam_Sise.shared.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "domain-events")
@Setting(settingPath = "/elasticsearch/domain-events-settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEventDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String eventType;

    @Field(type = FieldType.Text)
    private String eventData;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant occurredAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant storedAt;

    @Field(type = FieldType.Keyword)
    private String aggregateId;

    @Field(type = FieldType.Keyword)
    private String aggregateType;
}
