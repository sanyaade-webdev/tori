/*
 * Copyright 2012 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.vaadin.tori.data.entity;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portlet.messageboards.model.MBCategory;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.model.MBThread;
import com.liferay.portlet.ratings.model.RatingsEntry;

public class EntityFactoryUtil {

    public static Category createCategory(final MBCategory liferayCategory) {
        final Category entity = new Category();
        entity.setId(liferayCategory.getCategoryId());
        entity.setName(liferayCategory.getName());
        entity.setDescription(liferayCategory.getDescription());
        return entity;
    }

    public static void copyFields(final Category from, final MBCategory to) {
        to.setCategoryId(from.getId());
        to.setName(from.getName());
        to.setDescription(from.getDescription());
        if (from.getParentCategory() != null) {
            to.setParentCategoryId(from.getParentCategory().getId());
        }
    }

    public static List<Category> createCategories(
            final List<MBCategory> liferayCategories) {
        final List<Category> entities = new ArrayList<Category>(
                liferayCategories.size());
        for (final MBCategory liferayCategory : liferayCategories) {
            entities.add(createCategory(liferayCategory));
        }
        return entities;
    }

    public static DiscussionThread createDiscussionThread(
            final MBThread liferayThread, final MBMessage threadRootMessage,
            final User threadAuthor, final User lastPostAuthor) {
        final DiscussionThread entity = new DiscussionThread() {
            @Override
            public Post getLatestPost() {
                // TODO workaround for this hack
                final Post fakedLastPost = new Post();
                fakedLastPost.setTime(liferayThread.getLastPostDate());
                fakedLastPost.setAuthor(lastPostAuthor);
                return fakedLastPost;
            }

            @Override
            public User getOriginalPoster() {
                // TODO workaround for this hack
                return threadAuthor;
            }
        };
        entity.setId(liferayThread.getThreadId());
        entity.setTopic(threadRootMessage.getSubject());
        entity.setPostCount(liferayThread.getMessageCount());
        entity.setLocked(liferayThread.isLocked());
        entity.setViewCount(liferayThread.getViewCount());
        return entity;
    }

    public static Post createPost(final MBMessage liferayMessage,
            final User author, final DiscussionThread thread,
            final List<Attachment> attachments) {
        final Post entity = new Post();
        entity.setId(liferayMessage.getMessageId());
        entity.setTime(liferayMessage.getCreateDate());
        entity.setBodyRaw(liferayMessage.getBody(false));
        entity.setThread(thread);
        entity.setAuthor(author);
        entity.setAttachments(attachments);
        return entity;
    }

    @SuppressWarnings("deprecation")
    public static User createUser(
            final com.liferay.portal.model.User liferayUser,
            final String imagePath, final boolean isFemale,
            final boolean isBanned) {
        final User entity = new User();
        entity.setId(liferayUser.getUserId());
        entity.setDisplayedName(liferayUser.getFullName());
        entity.setAvatarUrl(getAvatarUrl(liferayUser.getPortraitId(),
                imagePath, isFemale));
        entity.setBanned(isBanned);
        return entity;
    }

    public static User createAnonymousUser(final String imagePath) {
        final User entity = new User();
        entity.setId(0);
        entity.setDisplayedName("Anonymous");
        entity.setAvatarUrl(getAvatarUrl(0, imagePath, false));
        return entity;
    }

    public static PostVote createPostVote(final RatingsEntry entry) {
        final PostVote vote = new PostVote();
        if (entry != null) {
            if (entry.getScore() > 0) {
                vote.setUpvote();
            } else {
                vote.setDownvote();
            }
        }
        return vote;
    }

    private static String getAvatarUrl(final long liferayPortraidId,
            final String imagePath, final boolean isFemale) {
        if (imagePath != null) {
            return imagePath + "/user_" + (isFemale ? "female" : "male")
                    + "_portrait?img_id=" + liferayPortraidId;
        }
        return null;
    }

}
