package org.ednovo.gooru.domain.service.collection;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ednovo.gooru.application.util.GooruImageUtil;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.CollectionType;
import org.ednovo.gooru.core.api.model.ContentMeta;
import org.ednovo.gooru.core.api.model.ContentSettings;
import org.ednovo.gooru.core.api.model.MetaConstants;
import org.ednovo.gooru.core.api.model.ResourceType;
import org.ednovo.gooru.core.api.model.Sharing;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.ParameterProperties;
import org.ednovo.gooru.infrastructure.messenger.IndexHandler;
import org.ednovo.gooru.infrastructure.messenger.IndexProcessor;
import org.ednovo.gooru.infrastructure.persistence.hibernate.CollectionDao;
import org.ednovo.gooru.infrastructure.persistence.hibernate.CollectionRepository;
import org.ednovo.gooru.security.OperationAuthorizer;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.type.TypeReference;

import flexjson.JSONSerializer;

@Service
public class CollectionBoServiceImpl extends AbstractCollectionServiceImpl implements CollectionBoService, ParameterProperties, ConstantProperties {

	@Autowired
	private CollectionDao collectionDao;

	@Autowired
	private IndexHandler indexHandler;

	@Autowired
	private OperationAuthorizer operationAuthorizer;
	
	@Autowired
	private CollectionRepository CollectionRepository;

	public CollectionRepository getCollectionRepository() {
		return CollectionRepository;
	}

	@Autowired
	private GooruImageUtil gooruImageUtil;

	private final static String COLLECTION_IMAGE_DIMENSION = "160x120,75x56,120x90,80x60,800x600";

	private final static String DEPTHOF_KNOWLEDGE = "depthOfKnowledge";

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public ActionResponseDTO<Collection> createCollection(User user, Collection collection) {
		if (collection.getBuildTypeId() != null) {
			collection.setBuildTypeId(Constants.BUILD_WEB_TYPE_ID);
		}
		final Errors errors = validateCollection(collection);
		if (!errors.hasErrors()) {
			Collection parentCollection = getCollectionDao().getCollection(user.getPartyUid(), CollectionType.SHElf.getCollectionType());
			if (parentCollection == null) {
				parentCollection = new Collection();
				parentCollection.setCollectionType(CollectionType.SHElf.getCollectionType());
				parentCollection.setTitle(CollectionType.SHElf.getCollectionType());
				super.createCollection(parentCollection, user);
				createCollection(user, collection, parentCollection);
			}
		}
		return new ActionResponseDTO<Collection>(collection, errors);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void deleteCollection(String courseId, String unitId, String lessonId, String collectionId) {
		Collection course = getCollectionDao().getCollectionByType(courseId, COURSE);
		rejectIfNull(course, GL0056, COURSE);
		Collection unit = getCollectionDao().getCollectionByType(unitId, UNIT);
		rejectIfNull(unit, GL0056, UNIT);
		Collection lesson = getCollectionDao().getCollectionByType(lessonId, LESSON);
		rejectIfNull(lesson, GL0056, LESSON);
		Collection collection = this.getCollectionDao().getCollection(collectionId);
		rejectIfNull(lesson, GL0056, COLLECTION);
		this.deleteCollection(collectionId);
		this.updateMetaDataSummary(course.getContentId(), unit.getContentId(), lesson.getContentId(), collection.getCollectionType(), DELETE);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public ActionResponseDTO<Collection> createCollection(String courseId, String unitId, String lessonId, User user, Collection collection) {
		final Errors errors = validateCollection(collection);
		if (!errors.hasErrors()) {
			Collection course = getCollectionDao().getCollectionByType(courseId, COURSE);
			rejectIfNull(course, GL0056, COURSE);
			Collection unit = getCollectionDao().getCollectionByType(unitId, UNIT);
			rejectIfNull(unit, GL0056, UNIT);
			Collection lesson = getCollectionDao().getCollectionByType(lessonId, LESSON);
			rejectIfNull(lesson, GL0056, LESSON);
			createCollection(user, collection, lesson);
			Map<String, Object> data = generateCollectionMetaData(collection, collection, user);
			data.put(SUMMARY, MetaConstants.COLLECTION_SUMMARY);
			createContentMeta(collection, data);
			updateMetaDataSummary(course.getContentId(), unit.getContentId(), lesson.getContentId(), collection.getCollectionType(), ADD);
		}
		return new ActionResponseDTO<Collection>(collection, errors);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void updateCollection(String collectionId, Collection newCollection, User user) {
		boolean hasUnrestrictedContentAccess = this.getOperationAuthorizer().hasUnrestrictedContentAccess(collectionId, user);
		Collection collection = null;
		if (hasUnrestrictedContentAccess) {
			collection = getCollectionDao().getCollection(collectionId);
		} else {
			collection = getCollectionDao().getCollectionByUser(collectionId, user.getPartyUid());
		}

		if (newCollection.getSharing() != null && (newCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.ANYONEWITHLINK.getSharing()))) {
			if (!newCollection.getSharing().equalsIgnoreCase(PUBLIC)) {
				collection.setPublishStatusId(null);
			}
			collection.setSharing(newCollection.getSharing());
		}
		if (newCollection.getSettings() != null) {
			updateCollectionSettings(collection, newCollection);
		}
		if (hasUnrestrictedContentAccess) {
			if (newCollection.getCreator() != null && newCollection.getCreator().getPartyUid() != null) {
				User creatorUser = getUserService().findByGooruId(newCollection.getCreator().getPartyUid());
				collection.setCreator(creatorUser);
			}
			if (newCollection.getUser() != null && newCollection.getUser().getPartyUid() != null) {
				User ownerUser = getUserService().findByGooruId(newCollection.getUser().getPartyUid());
				collection.setUser(ownerUser);
			}
			if (newCollection.getNetwork() != null) {
				collection.setNetwork(newCollection.getNetwork());
			}
		}
		if (newCollection.getMediaFilename() != null) {
			String folderPath = Collection.buildResourceFolder(collection.getContentId());
			this.getGooruImageUtil().imageUpload(newCollection.getMediaFilename(), folderPath, COLLECTION_IMAGE_DIMENSION);
			StringBuilder basePath = new StringBuilder(folderPath);
			basePath.append(File.separator).append(newCollection.getMediaFilename());
			collection.setImagePath(basePath.toString());
		}
		updateCollection(collection, newCollection, user);
		Map<String, Object> data = generateCollectionMetaData(collection, newCollection, user);
		if (data != null && data.size() > 0) {
			ContentMeta contentMeta = this.getContentRepository().getContentMeta(collection.getContentId());
			updateContentMeta(contentMeta, data);
		}
	}

	private Collection createCollection(User user, Collection collection, Collection parentCollection) {
		createCollection(collection, parentCollection, user);
		if (collection.getSharing() != null && !collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) && collection.getSharing().equalsIgnoreCase(PUBLIC)) {
			collection.setSharing(Sharing.ANYONEWITHLINK.getSharing());
		}
		if (collection.getSharing() != null && !collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) && collection.getSharing().equalsIgnoreCase(PUBLIC)) {
			collection.setPublishStatusId(Constants.PUBLISH_PENDING_STATUS_ID);
			collection.setSharing(Sharing.ANYONEWITHLINK.getSharing());
		}
		createCollectionSettings(collection);
		if (!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType())) {
			indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);
		}
		return collection;
	}

	private void createCollectionSettings(Collection collection) {
		final ContentSettings contentSetting = new ContentSettings();
		if (collection.getSettings() == null || collection.getSettings().size() == 0) {
			collection.setSettings(Constants.COLLECTION_DEFAULT_SETTINGS);
		}
		contentSetting.setContent(collection);
		contentSetting.setData(new JSONSerializer().exclude(EXCLUDE).serialize(collection.getSettings()));
		getCollectionDao().save(contentSetting);
	}

	private void updateCollectionSettings(Collection collection, Collection newCollection) {
		ContentSettings contentSettings = null;
		final Map<String, String> settings = new HashMap<String, String>();
		if (collection.getContentSettings() != null && collection.getContentSettings().size() > 0) {
			contentSettings = collection.getContentSettings().iterator().next();
			final Map<String, String> contentSettingsMap = JsonDeserializer.deserialize(contentSettings.getData(), new TypeReference<Map<String, String>>() {
			});
			settings.putAll(contentSettingsMap);
		}
		settings.putAll(newCollection.getSettings());
		newCollection.setSettings(settings);
		ContentSettings contentSetting = contentSettings == null ? new ContentSettings() : contentSettings;
		contentSetting.setContent(collection);
		contentSetting.setData(new JSONSerializer().exclude(EXCLUDE).serialize(newCollection.getSettings()));
		this.getCollectionDao().save(contentSetting);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public Map<String, Object> getCollection(String collectionId, String collectionType) {
		return super.getCollection(collectionId, collectionType);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public List<Map<String, Object>> getCollections(String lessonId, String collectionType, int limit, int offset) {
		Map<String, Object> filters = new HashMap<String, Object>();
		String[] collectionTypes = collectionType.split(",");
		filters.put(COLLECTION_TYPE, collectionTypes);
		filters.put(PARENT_GOORU_ID, lessonId);
		return this.getCollections(filters, limit, offset);
	}

	@SuppressWarnings("unchecked")
	private void updateMetaDataSummary(Long courseId, Long unitId, Long lessonId, String collectionType, String action) {
		ContentMeta unitContentMeta = this.getContentRepository().getContentMeta(unitId);
		ContentMeta courseContentMeta = this.getContentRepository().getContentMeta(courseId);
		ContentMeta lessonContentMeta = this.getContentRepository().getContentMeta(lessonId);
		if (lessonContentMeta != null) {
			int assessmentCount = this.getCollectionDao().getCollectionItemCount(lessonId, CollectionType.ASSESSMENT.getCollectionType());
			int collectionCount = this.getCollectionDao().getCollectionItemCount(lessonId, CollectionType.COLLECTION.getCollectionType());
			Map<String, Object> metaData = JsonDeserializer.deserialize(lessonContentMeta.getMetaData(), new TypeReference<Map<String, Object>>() {
			});
			Map<String, Object> summary = (Map<String, Object>) metaData.get(SUMMARY);
			summary.put(MetaConstants.COLLECTION_COUNT, collectionCount);
			summary.put(MetaConstants.ASSESSMENT_COUNT, assessmentCount);
			metaData.put(SUMMARY, summary);
			updateContentMeta(lessonContentMeta, metaData);
		}

		if (unitContentMeta != null) {
			updateSummaryMeta(collectionType, unitContentMeta, action);
		}
		if (courseContentMeta != null) {
			updateSummaryMeta(collectionType, courseContentMeta, action);
		}

	}

	@SuppressWarnings("unchecked")
	private void updateSummaryMeta(String collectionType, ContentMeta contentMeta, String action) {
		Map<String, Object> metaData = JsonDeserializer.deserialize(contentMeta.getMetaData(), new TypeReference<Map<String, Object>>() {
		});
		Map<String, Object> summary = (Map<String, Object>) metaData.get(SUMMARY);
		if (collectionType.equalsIgnoreCase(CollectionType.ASSESSMENT.getCollectionType())) {
			int assessmentCount = 0;
			if(action.equalsIgnoreCase(DELETE)){
				assessmentCount = ((Number) summary.get(MetaConstants.ASSESSMENT_COUNT)).intValue() - 1;
			}else if(action.equalsIgnoreCase(ADD)){
				assessmentCount = ((Number) summary.get(MetaConstants.ASSESSMENT_COUNT)).intValue() + 1;
			}
			summary.put(MetaConstants.ASSESSMENT_COUNT, assessmentCount);
		}
		if (collectionType.equalsIgnoreCase(CollectionType.COLLECTION.getCollectionType())) {
			int collectionCount = 0;
			if(action.equalsIgnoreCase(DELETE)){
				collectionCount = ((Number) summary.get(MetaConstants.COLLECTION_COUNT)).intValue() - 1;
			}else if(action.equalsIgnoreCase(ADD)){
				collectionCount = ((Number) summary.get(MetaConstants.COLLECTION_COUNT)).intValue() + 1;
			}
			summary.put(MetaConstants.COLLECTION_COUNT, collectionCount);
		}
		metaData.put(SUMMARY, summary);
		updateContentMeta(contentMeta, metaData);
	}

	private Map<String, Object> generateCollectionMetaData(Collection collection, Collection newCollection, User user) {
		Map<String, Object> data = new HashMap<String, Object>();
		if (newCollection.getSkillIds() != null) {
			List<Map<String, Object>> skills = updateContentCode(collection, newCollection.getSkillIds(), MetaConstants.CONTENT_CLASSIFICATION_SKILLS_TYPE_ID);
			data.put(SKILLS, skills);
		}
		if (newCollection.getAudienceIds() != null) {
			List<Map<String, Object>> audiences = updateContentMetaAssoc(collection, user, AUDIENCE, newCollection.getAudienceIds());
			data.put(AUDIENCE, audiences);
		}
		if (newCollection.getDepthOfKnowledgeIds() != null) {
			List<Map<String, Object>> depthOfKnowledge = updateContentMetaAssoc(collection, user, DEPTH_OF_KNOWLEDGE, newCollection.getDepthOfKnowledgeIds());
			data.put(DEPTHOF_KNOWLEDGE, depthOfKnowledge);
		}
		return data;
	}

	private Errors validateCollection(final Collection collection) {
		final Errors errors = new BindException(collection, COLLECTION);
		if (collection != null) {
			rejectIfNullOrEmpty(errors, collection.getTitle(), TITLE, GL0006, generateErrorMessage(GL0006, TITLE));
			rejectIfInvalidType(errors, collection.getCollectionType(), COLLECTION_TYPE, GL0007, generateErrorMessage(GL0007, COLLECTION_TYPE), Constants.COLLECTION_TYPES);
			if (collection.getPublishStatusId() != null) {
				rejectIfInvalidType(errors, collection.getPublishStatusId(), PUBLISH_STATUS, GL0007, generateErrorMessage(GL0007, PUBLISH_STATUS), Constants.PUBLISH_STATUS);
			}
		}
		return errors;
	}

	public CollectionDao getCollectionDao() {
		return collectionDao;
	}

	public IndexHandler getIndexHandler() {
		return indexHandler;
	}

	public OperationAuthorizer getOperationAuthorizer() {
		return operationAuthorizer;
	}

	public GooruImageUtil getGooruImageUtil() {
		return gooruImageUtil;
	}
}
