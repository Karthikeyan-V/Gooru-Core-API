package org.ednovo.gooru.core.api.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import flexjson.transformer.ObjectTransformer;

public class ContentPermissionTransformer extends ObjectTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContentPermissionTransformer.class); 
	
	@Override
	public void transform(Object object) {
		ContentPermission contentPermission = (ContentPermission) object;
		if (contentPermission != null) {

			ContentPermissionTransModel organizationModel = new ContentPermissionTransModel();
			organizationModel.setPartyUid(contentPermission.getParty().getPartyUid());

			getContext().transform(organizationModel);

		} else {
			LOGGER.error("Serialization failed for content permission transformer");
			getContext().write(null);
		}
	}

}


