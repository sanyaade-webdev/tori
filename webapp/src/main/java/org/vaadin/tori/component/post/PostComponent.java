package org.vaadin.tori.component.post;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.vaadin.tori.ToriApplication;
import org.vaadin.tori.ToriNavigator;
import org.vaadin.tori.ToriRoot;
import org.vaadin.tori.ToriUtil;
import org.vaadin.tori.component.ConfirmationDialog;
import org.vaadin.tori.component.ConfirmationDialog.ConfirmationListener;
import org.vaadin.tori.component.ContextMenu;
import org.vaadin.tori.component.MenuPopup.ContextAction;
import org.vaadin.tori.component.MenuPopup.ContextComponentSwapper;
import org.vaadin.tori.component.post.EditComponent.EditListener;
import org.vaadin.tori.data.entity.Attachment;
import org.vaadin.tori.data.entity.Post;
import org.vaadin.tori.data.entity.PostVote;
import org.vaadin.tori.data.entity.User;
import org.vaadin.tori.exception.DataSourceException;
import org.vaadin.tori.thread.ThreadPresenter;

import com.ocpsoft.pretty.time.PrettyTime;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.gwt.client.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.themes.BaseTheme;

import edu.umd.cs.findbugs.annotations.CheckForNull;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SE_BAD_FIELD", justification = "We don't bother us with serialization.")
public class PostComponent extends CustomComponent {

    private static final String UNFOLLOW_CAPTION = "Unfollow Thread";
    private static final ThemeResource UNFOLLOW_ICON = new ThemeResource(
            "images/icon-unfollow.png");

    private static final String FOLLOW_CAPTION = "Follow Thread";
    private static final ThemeResource FOLLOW_ICON = new ThemeResource(
            "images/icon-follow.png");

    private final PrettyTime prettyTime = new PrettyTime();
    private final DateFormat dateFormat = new SimpleDateFormat(
            "MM/dd/yyyy kk:mm");

    // trying a new pattern here by grouping auxiliary methods in an inner class
    private static class Util {
        private static Component newConfirmBanComponent(
                final ThreadPresenter presenter, final User user,
                final ContextMenu menu) {
            final String title = String.format("Ban %s?",
                    user.getDisplayedName());
            final String confirmCaption = "Yes, Ban";
            final String cancelCaption = "No, Cancel!";
            final ConfirmationListener listener = new ConfirmationListener() {

                @Override
                public void onConfirmed() throws DataSourceException {
                    presenter.ban(user);
                    menu.close();
                }

                @Override
                public void onCancel() {
                    menu.close();
                }
            };
            return new ConfirmationDialog(title, confirmCaption, cancelCaption,
                    listener);
        }

        public static Component newConfirmDeleteComponent(
                final ThreadPresenter presenter, final Post post,
                final ContextMenu menu) {
            final String title = String.format("Delete Post?");
            final String confirmCaption = "Yes, Delete";
            final String cancelCaption = "No, Cancel!";
            final ConfirmationListener listener = new ConfirmationListener() {

                @Override
                public void onConfirmed() throws DataSourceException {
                    presenter.delete(post);
                    menu.close();
                }

                @Override
                public void onCancel() {
                    menu.close();
                }
            };
            return new ConfirmationDialog(title, confirmCaption, cancelCaption,
                    listener);
        }
    }

    private final CustomLayout root;
    private final Post post;

    private final EditListener editListener = new EditListener() {
        @Override
        public void postEdited(final String newPostBody) {
            try {
                presenter.saveEdited(post, newPostBody);
                getRoot().trackAction(null, "edit-post");
                // this component will be replaced with a new one. So no need to
                // change the state.
            } catch (final DataSourceException e) {
                getRoot().showNotification(
                        DataSourceException.BORING_GENERIC_ERROR_MESSAGE,
                        Notification.TYPE_ERROR_MESSAGE);
            }
        }
    };

    private final ClickListener replyListener = new ClickListener() {
        @Override
        public void buttonClick(final ClickEvent event) {
            if (event.getButton().getData() instanceof Post) {
                final Post postToQuote = (Post) event.getButton().getData();
                presenter.quotePost(postToQuote);
            }
        }
    };

    private final Component reportComponent;
    private final Button quoteButton;
    private final Button scrollToButton;
    private final ContextMenu contextMenu;
    private final ThreadPresenter presenter;
    private final PostScoreComponent score;
    private Component scrollToComponent;

    private final ContextAction followAction = new ContextAction() {
        @Override
        public void contextClicked() {
            try {
                presenter.followThread();
            } catch (final DataSourceException e) {
                getRoot().showNotification(
                        DataSourceException.BORING_GENERIC_ERROR_MESSAGE);
            }
        }
    };

    private final ContextAction unfollowAction = new ContextAction() {
        @Override
        public void contextClicked() {
            try {
                presenter.unFollowThread();
            } catch (final DataSourceException e) {
                getRoot().showNotification(
                        DataSourceException.BORING_GENERIC_ERROR_MESSAGE);
            }

        }
    };

    private boolean followingEnabled = false;
    private boolean unfollowingEnabled = false;
    private final EditComponent editComponent;
    private final boolean allowHtml;

    /**
     * @throws IllegalArgumentException
     *             if any argument is <code>null</code>.
     */
    public PostComponent(final Post post, final ThreadPresenter presenter) {
        this(post, presenter, true);
    }

    /**
     * @throws IllegalArgumentException
     *             if any argument is <code>null</code>.
     */
    public PostComponent(final Post post, final ThreadPresenter presenter,
            final boolean allowHtml) {

        ToriUtil.checkForNull(post, "post may not be null");
        ToriUtil.checkForNull(presenter, "presenter may not be null");

        this.presenter = presenter;
        this.post = post;
        this.allowHtml = allowHtml;

        root = new CustomLayout("postlayout");
        setCompositionRoot(root);
        setStyleName("post");

        editComponent = new EditComponent(post.getBodyRaw(), editListener);
        editComponent.setVisible(false);

        quoteButton = new Button("Quote for Reply", replyListener);
        quoteButton.setData(post);
        quoteButton.setStyleName(BaseTheme.BUTTON_LINK);
        quoteButton.setIcon(new ThemeResource("images/icon-link-reply.png"));
        quoteButton.setVisible(false);

        contextMenu = new ContextMenu();
        score = new PostScoreComponent(post, presenter);
        try {
            score.setScore(presenter.getScore(post));
        } catch (final DataSourceException e) {
            // NOP - logged, just showing a score of OVER 9000!!!!
            score.setScore(9001);
        }

        scrollToButton = new Button("Scroll to Post");
        scrollToButton.setStyleName(BaseTheme.BUTTON_LINK);
        scrollToButton.setVisible(false);
        scrollToButton.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                if (scrollToComponent != null) {
                    getRoot().scrollIntoView(scrollToComponent);
                }
            }
        });
        root.addComponent(scrollToButton, "scrollto");

        root.addComponent(getAvatarImage(post), "avatar");
        root.addComponent(new Label(post.getAuthor().getDisplayedName()),
                "authorname");
        root.addComponent(getPostedAgoLabel(post), "postedtime");
        root.addComponent(getPermaLink(post), "permalink");

        final String rawSignature = post.getAuthor().getSignatureRaw();
        if (rawSignature != null && !rawSignature.isEmpty()) {
            final String formattedSignature = ToriApplication.getCurrent()
                    .getSignatureFormatter().format(rawSignature);
            root.addComponent(new Label(formattedSignature, ContentMode.XHTML),
                    "signature");
        }

        refreshBody(post);

        reportComponent = new ReportComponent(post, presenter,
                getPermaLinkUrl(post));
        reportComponent.setVisible(false);

        final Component attachments = getAttachments(post);
        if (attachments != null) {
            root.addComponent(attachments, "attachments");
        }
        root.addComponent(score, "score");
        root.addComponent(reportComponent, "report");
        root.addComponent(contextMenu, "settings");
        root.addComponent(editComponent, "edit");
        root.addComponent(quoteButton, "quote");
    }

    public void setScrollToComponent(final Component scrollTo) {
        scrollToComponent = scrollTo;
        scrollToButton.setVisible(scrollTo != null);
    }

    public void enableReporting() {
        reportComponent.setVisible(true);
    }

    public void enableEditing() {
        editComponent.setVisible(true);
    }

    public void enableQuoting() {
        quoteButton.setVisible(true);
    }

    public void enableThreadFollowing() {
        contextMenu.add(FOLLOW_ICON, FOLLOW_CAPTION, followAction);
        followingEnabled = true;
        unfollowingEnabled = false;
    }

    public void enableThreadUnFollowing() {
        contextMenu.add(UNFOLLOW_ICON, UNFOLLOW_CAPTION, unfollowAction);
        followingEnabled = false;
        unfollowingEnabled = true;
    }

    public void enableBanning() {
        contextMenu.add(new ThemeResource("images/icon-ban.png"), "Ban Author",
                new ContextComponentSwapper() {
                    @Override
                    public Component swapContextComponent() {
                        return Util.newConfirmBanComponent(presenter,
                                post.getAuthor(), contextMenu);
                    }
                });
    }

    public void enableDeleting() {
        contextMenu.add(new ThemeResource("images/icon-delete.png"),
                "Delete Post", new ContextComponentSwapper() {
                    @Override
                    public Component swapContextComponent() {
                        return Util.newConfirmDeleteComponent(presenter, post,
                                contextMenu);
                    }
                });
    }

    public void enableUpDownVoting(final PostVote postVote) {
        score.enableUpDownVoting(postVote);
    }

    /*-
    private Component buildReportPostComponent(final Post post,
            final ThreadPresenter presenter) {
        final Button button = new Button("Report Post");
        button.setStyleName(Reindeer.BUTTON_LINK);
        button.setIcon(new ThemeResource("images/icon-link-report.png"));
        button.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                final int x = event.getClientX();
                final int y = event.getClientY();
                getRoot()
                        .addWindow(
                                new ReportWindow(post, presenter,
                                        getPermaLinkUrl(post)));
            }
        });
        button.setVisible(false);
        return button;
    }
     */

    @CheckForNull
    private Component getAttachments(final Post post) {
        if (post.hasAttachments()) {
            final CssLayout attachmentLinks = new CssLayout();
            attachmentLinks.setCaption("Attachments");

            // create a Link for each attachment
            for (final Attachment attachment : post.getAttachments()) {
                final String linkCaption = String.format("%s (%s KB)",
                        attachment.getFilename(),
                        attachment.getFileSize() / 1024);
                attachmentLinks.addComponent(new Link(linkCaption,
                        new ExternalResource(attachment.getDownloadUrl())));
            }
            return attachmentLinks;
        }
        return null;
    }

    private String getFormattedXhtmlBody(final Post post) {
        return ToriApplication.getCurrent().getPostFormatter()
                .format(post.getBodyRaw());
    }

    private static String getPermaLinkUrl(final Post post) {
        // @formatter:off
        final String linkUrl = String.format(
                "#%s/%s/%s",
                ToriNavigator.ApplicationView.THREADS.getUrl(), 
                post.getThread().getId(),
                post.getId()
                );
        // @formatter:on

        return linkUrl;
    }

    private static Component getPermaLink(final Post post) {
        // @formatter:off
        final String linkString = String.format(
                "<a href=\"%s\">Permalink</a>",
                getPermaLinkUrl(post)
                );
        // @formatter:on

        final Label label = new Label(linkString, ContentMode.XHTML);
        return label;
    }

    private Label getPostedAgoLabel(final Post post) {
        final StringBuilder xhtml = new StringBuilder();
        xhtml.append("<span class=\"prettytime\">");
        xhtml.append(prettyTime.format(post.getTime()));
        xhtml.append("</span><span class=\"timestamp\">");
        xhtml.append(dateFormat.format(post.getTime()));
        xhtml.append("</span>");
        return new Label(xhtml.toString(), ContentMode.XHTML);
    }

    private Embedded getAvatarImage(final Post post) {
        final String avatarUrl = post.getAuthor().getAvatarUrl();

        final Resource imageResource;
        if (avatarUrl != null) {
            imageResource = new ExternalResource(avatarUrl);
        } else {
            imageResource = new ThemeResource(
                    "images/icon-placeholder-avatar.gif");
        }

        final Embedded image = new Embedded(null, imageResource);
        image.setType(Embedded.TYPE_IMAGE);
        image.setWidth("90px");
        return image;
    }

    public void refreshScores(final long newScore) {
        score.setScore(newScore);

        try {
            // just to refresh the up/down icon visuals. bad method name here.
            score.enableUpDownVoting(presenter.getPostVote(post));
        } catch (final DataSourceException e) {
            getRoot().showNotification(
                    DataSourceException.BORING_GENERIC_ERROR_MESSAGE);
        }
    }

    public void swapFollowingMenu() {
        if (followingEnabled || unfollowingEnabled) {

            if (followingEnabled) {
                contextMenu.swap(followAction, UNFOLLOW_ICON, UNFOLLOW_CAPTION,
                        unfollowAction);
            } else {
                contextMenu.swap(unfollowAction, FOLLOW_ICON, FOLLOW_CAPTION,
                        followAction);
            }

            followingEnabled = !followingEnabled;
            unfollowingEnabled = !unfollowingEnabled;
        }
    }

    public final void refreshBody(final Post post) {
        root.removeComponent("body");
        final String formattedPost = getFormattedXhtmlBody(post);
        if (allowHtml) {
            root.addComponent(new Label(formattedPost, ContentMode.XHTML),
                    "body");
        } else {
            root.addComponent(new Label(presenter.stripTags(formattedPost),
                    ContentMode.XHTML), "body");
        }
    }

    @Override
    public ToriRoot getRoot() {
        return (ToriRoot) super.getRoot();
    }
}
