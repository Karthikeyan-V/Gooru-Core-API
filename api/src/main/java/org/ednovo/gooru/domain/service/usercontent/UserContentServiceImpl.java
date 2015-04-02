/////////////////////////////////////////////////////////////
// UserContentServiceImpl.java
// gooru-api
// Created by Gooru on 2014
// Copyright (c) 2014 Gooru. All rights reserved.
// http://www.goorulearning.org/
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/////////////////////////////////////////////////////////////
package org.ednovo.gooru.domain.service.usercontent;

import java.util.List;

import org.ednovo.gooru.core.api.model.UserContentAssoc;
import org.ednovo.gooru.infrastructure.persistence.hibernate.UserContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userContentService")
public class UserContentServiceImpl implements UserContentService {
	
	@Autowired
	private UserContentRepository userContentRepository;
	
	
	@Override
	public List<UserContentAssoc> listContentUserRelations(String contentGooruOId) {
		return userContentRepository.listContentUserRelations(contentGooruOId);
	}
	
	@Override
	public UserContentAssoc getUserContentAssoc(String userGooruId,String contentGooruId,Integer relationId) {
		return userContentRepository.getUserContentAssoc(userGooruId, contentGooruId, relationId);
	}
	
	@Override
	public UserContentAssoc getUserContentAssoc(String userId,Long contentId,Integer relationId) {
		return userContentRepository.getUserContentAssoc(userId, contentId, relationId);
	}
	
	@Override
	public void deleteUserContentRelationShip(UserContentAssoc userContentAssoc) {
		userContentRepository.deleteUserContentRelationShip(userContentAssoc);
	}

}

