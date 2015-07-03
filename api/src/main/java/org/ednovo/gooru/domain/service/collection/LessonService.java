package org.ednovo.gooru.domain.service.collection;

import java.util.List;
import java.util.Map;

import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.User;

public interface LessonService extends AbstractCollectionService {
	ActionResponseDTO<Collection> createLesson(String courseId, String unitId, Collection collection, User user);

	public void updateLesson(String courseUId, String lessonId, Collection newCollection, User user);

	Map<String, Object> getLesson(String lessonId);

	List<Map<String, Object>> getLessons(String unitId, int limit, int offset);
	
	void deleteLesson(String courseUId, String unitUId, String lessonUId,
			User user);

}
