package ardaaydinkilinc.Cam_Sise.shared.infrastructure.event;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainEventSearchRepository extends ElasticsearchRepository<DomainEventDocument, String> {
}
