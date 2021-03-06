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

package org.vaadin.tori.category;

import java.util.Collections;
import java.util.List;

import org.vaadin.tori.category.CategoryView.ThreadProvider;
import org.vaadin.tori.data.DataSource;
import org.vaadin.tori.data.entity.Category;
import org.vaadin.tori.data.entity.DiscussionThread;
import org.vaadin.tori.exception.DataSourceException;
import org.vaadin.tori.mvp.Presenter;
import org.vaadin.tori.service.AuthorizationService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class CategoryPresenter extends Presenter<CategoryView> {

    private Category currentCategory;

    /* package protected due to testing */
    final ThreadProvider recentPostsProvider = new ThreadProvider() {
        @Override
        @NonNull
        public List<DiscussionThread> getThreadsBetween(final int from,
                final int to) throws DataSourceException {
            final List<DiscussionThread> recentPosts = dataSource
                    .getRecentPosts(from, to);
            return recentPosts;
        }

        @Override
        public int getThreadAmount() throws DataSourceException {
            final int recentPostsAmount = dataSource.getRecentPostsAmount();
            return recentPostsAmount;
        }
    };

    /* package protected due to testing */
    final ThreadProvider myPostsProvider = new ThreadProvider() {
        @Override
        @NonNull
        public List<DiscussionThread> getThreadsBetween(final int from,
                final int to) throws DataSourceException {
            return dataSource.getMyPosts(from, to);
        }

        @Override
        public int getThreadAmount() throws DataSourceException {
            return dataSource.getMyPostsAmount();
        }
    };

    /* package protected due to testing */
    final ThreadProvider defaultThreadsProvider = new ThreadProvider() {
        @Override
        @NonNull
        public List<DiscussionThread> getThreadsBetween(final int from,
                final int to) throws DataSourceException {
            return dataSource.getThreads(currentCategory, from, to);
        }

        @Override
        public int getThreadAmount() throws DataSourceException {
            return (int) dataSource.getThreadCount(currentCategory);
        }
    };

    public CategoryPresenter(final DataSource dataSource,
            final AuthorizationService authorizationService) {
        super(dataSource, authorizationService);
    }

    public void setCurrentCategoryById(final String categoryIdString) {
        final CategoryView view = getView();
        try {
            if (categoryIdString.equals(SpecialCategory.RECENT_POSTS.getId())) {
                currentCategory = SpecialCategory.RECENT_POSTS.getInstance();

                final List<Category> empty = Collections.emptyList();
                view.displaySubCategories(empty);
                view.displayThreads(recentPostsProvider);
            } else if (categoryIdString
                    .equals(SpecialCategory.MY_POSTS.getId())) {
                currentCategory = SpecialCategory.MY_POSTS.getInstance();

                final List<Category> empty = Collections.emptyList();
                view.displaySubCategories(empty);
                view.displayThreads(myPostsProvider);
            } else {
                Category requestedCategory = null;
                try {
                    final long categoryId = Long.valueOf(categoryIdString);
                    requestedCategory = dataSource.getCategory(categoryId);
                } catch (final NumberFormatException e) {
                    log.error("Invalid category id format: " + categoryIdString);
                }

                if (requestedCategory != null) {
                    currentCategory = requestedCategory;
                    view.displaySubCategories(dataSource
                            .getSubCategories(currentCategory));
                } else {
                    getView().displayCategoryNotFoundError(categoryIdString);
                }
                if (countThreads() > 0) {
                    view.displayThreads(defaultThreadsProvider);
                } else {
                    view.hideThreads();
                }
                view.setUserMayStartANewThread(userMayStartANewThread());
            }
        } catch (final DataSourceException e) {
            e.printStackTrace();
            getView().panic();
        }
    }

    /**
     * Might return <code>null</code> if the visited URL doesn't include a valid
     * category id.
     */
    @CheckForNull
    public Category getCurrentCategory() {
        return currentCategory;
    }

    /**
     * Returns <code>true</code> iff the current user doesn't follow the thread,
     * and is allowed to follow a thread.
     */
    public boolean userCanFollow(final DiscussionThread thread)
            throws DataSourceException {
        try {
            return authorizationService.mayFollow(thread)
                    && !dataSource.isFollowing(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Returns <code>true</code> iff the current user follows the thread, and
     * <em>is allowed to follow a thread</em>.
     */
    public boolean userCanUnFollow(final DiscussionThread thread)
            throws DataSourceException {
        try {
            return authorizationService.mayFollow(thread)
                    && dataSource.isFollowing(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void follow(final DiscussionThread thread)
            throws DataSourceException {
        try {
            dataSource.follow(thread);
            getView().confirmFollowing(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void unfollow(final DiscussionThread thread)
            throws DataSourceException {
        try {
            dataSource.unFollow(thread);
            getView().confirmUnfollowing(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public boolean userMayMove(final DiscussionThread thread) {
        return authorizationService.mayMove(thread);
    }

    public List<Category> getRootCategories() throws DataSourceException {
        try {
            return dataSource.getRootCategories();
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public List<Category> getSubCategories(final Category category)
            throws DataSourceException {
        try {
            return dataSource.getSubCategories(category);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void move(final DiscussionThread thread,
            final Category destinationCategory) throws DataSourceException {
        try {
            dataSource.move(thread, destinationCategory);
            getView().confirmThreadMoved();
            resetView();
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void sticky(final DiscussionThread thread)
            throws DataSourceException {
        try {
            final DiscussionThread updatedThread = dataSource.sticky(thread);
            getView().confirmThreadStickied(updatedThread);
            resetView();
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void unsticky(final DiscussionThread thread)
            throws DataSourceException {
        try {
            final DiscussionThread updatedThread = dataSource.unsticky(thread);
            getView().confirmThreadUnstickied(updatedThread);
            resetView();
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public boolean userCanSticky(final DiscussionThread thread) {
        return authorizationService.maySticky(thread) && !thread.isSticky();
    }

    public boolean userCanUnSticky(final DiscussionThread thread) {
        return authorizationService.maySticky(thread) && thread.isSticky();
    }

    public void lock(final DiscussionThread thread) throws DataSourceException {
        try {
            final DiscussionThread updatedThread = dataSource.lock(thread);
            getView().confirmThreadLocked(updatedThread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public void unlock(final DiscussionThread thread)
            throws DataSourceException {
        try {
            final DiscussionThread updatedThread = dataSource.unlock(thread);
            getView().confirmThreadUnlocked(updatedThread);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public boolean userCanLock(final DiscussionThread thread) {
        return authorizationService.mayLock(thread) && !thread.isLocked();
    }

    public boolean userCanUnLock(final DiscussionThread thread) {
        return authorizationService.mayLock(thread) && thread.isLocked();
    }

    public boolean userMayDelete(final DiscussionThread thread) {
        return authorizationService.mayDelete(thread);
    }

    public void delete(final DiscussionThread thread)
            throws DataSourceException {
        try {
            dataSource.delete(thread);
            getView().confirmThreadDeleted();
            resetView();
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }

    }

    public boolean userMayStartANewThread() {
        boolean userMayStartANewThread = false;
        if (SpecialCategory.isSpecialCategory(currentCategory)) {
            // special "categories" like recent posts
            userMayStartANewThread = false;
        }
        if (authorizationService != null) {
            userMayStartANewThread = authorizationService
                    .mayCreateThreadIn(currentCategory);
        }
        return userMayStartANewThread;
    }

    private void resetView() throws DataSourceException {
        getView().displayThreads(defaultThreadsProvider);
    }

    public boolean userHasRead(final DiscussionThread thread) {
        try {
            return dataSource.isRead(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            // Just log the error and return true, not considering this a
            // serious problem.
            return true;
        }
    }

    public boolean userIsFollowing(final DiscussionThread thread) {
        try {
            return dataSource.isFollowing(thread);
        } catch (final DataSourceException e) {
            log.error(e);
            // Just log the error and return false, not considering this a
            // serious problem.
            return false;
        }
    }

    public long countThreads() throws DataSourceException {
        try {
            return dataSource.getThreadCount(currentCategory);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public List<DiscussionThread> getThreadsBetween(final int from, final int to)
            throws DataSourceException {
        try {
            return dataSource.getThreads(currentCategory, from, to);
        } catch (final DataSourceException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public boolean userCanCreateSubcategory() {
        return authorizationService.mayEditCategories();
    }
}
