package org.vaadin.tori.edit;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vaadin.tori.ToriApplication;
import org.vaadin.tori.mvp.AbstractView;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditViewImpl extends AbstractView<EditView, EditPresenter>
        implements EditView {

    private VerticalLayout layout;
    private Table replacementsTable;
    private Button removeButton;
    private Button saveButton;
    private Button newButton;

    private CheckBox convertMessageBoardsUrls;
    private TextField analyticsTrackerIdField;

    @Override
    protected Component createCompositionRoot() {
        layout = new VerticalLayout();
        return layout;
    }

    @Override
    public void initView() {
        layout.setSpacing(true);

        analyticsTrackerIdField = new TextField("Google Analytics Tracker id");
        layout.addComponent(analyticsTrackerIdField);

        layout.addComponent(new Label(
                "Define post body regex-patterns/replacements to be "
                        + "applied whenever posts are being displayed/previewed."));

        replacementsTable = new Table();
        replacementsTable.setWidth("100%");
        replacementsTable.setHeight("10em");
        replacementsTable.setSelectable(true);
        replacementsTable.setImmediate(true);
        replacementsTable.setEditable(true);
        replacementsTable.setTableFieldFactory(new TableFieldFactory() {

            @Override
            public Field<?> createField(final Container container,
                    final Object itemId, final Object propertyId,
                    final Component uiContext) {
                final TextField textField = new TextField();
                textField.setWidth(100.0f, Unit.PERCENTAGE);
                textField.addListener(new FocusListener() {
                    @Override
                    public void focus(final FocusEvent event) {
                        replacementsTable.setValue(itemId);
                    }
                });
                return textField;
            }
        });

        replacementsTable.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(final ValueChangeEvent event) {
                removeButton.setEnabled(event.getProperty().getValue() != null);
            }
        });

        replacementsTable.addContainerProperty("regex", String.class, "",
                "Regex", null, null);
        replacementsTable.setSortContainerPropertyId("regex");
        replacementsTable.addContainerProperty("replacement", String.class, "",
                "Replacement", null, null);
        layout.addComponent(replacementsTable);

        removeButton = new Button("Remove", new Button.ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                replacementsTable.removeItem(replacementsTable.getValue());
            }
        });
        removeButton.setEnabled(false);

        newButton = new Button("New", new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                final Object itemId = replacementsTable.addItem();
                replacementsTable.setValue(itemId);
            }
        });

        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);
        buttonsLayout.addComponent(removeButton);
        buttonsLayout.addComponent(newButton);
        layout.addComponent(buttonsLayout);

        layout.addComponent(new Label(
                "Check the box beneath to let Tori scan post content render-time for links "
                        + "intended for Liferay message boards portlet and convert them to tori-format for display."));
        convertMessageBoardsUrls = new CheckBox(
                "Replace message boards link data with tori format");
        layout.addComponent(convertMessageBoardsUrls);

        saveButton = new Button("Save preferences", new Button.ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Map<String, String> replacements = new HashMap<String, String>();

                for (final Object itemId : replacementsTable.getItemIds()) {
                    final Item item = replacementsTable.getItem(itemId);
                    replacements.put((String) item.getItemProperty("regex")
                            .getValue(),
                            (String) item.getItemProperty("replacement")
                                    .getValue());

                }

                final String trackerId = analyticsTrackerIdField.getValue();
                final String fixedTrackerId = "".equals(trackerId) ? null
                        : trackerId;

                getPresenter().savePreferences(replacements,
                        convertMessageBoardsUrls.getValue(), fixedTrackerId);
            }
        });
        layout.addComponent(saveButton);
        layout.setComponentAlignment(saveButton, Alignment.MIDDLE_RIGHT);
    }

    @Override
    protected EditPresenter createPresenter() {
        final ToriApplication app = ToriApplication.getCurrent();
        return new EditPresenter(app.getDataSource(),
                app.getAuthorizationService());
    }

    @Override
    public void setReplacements(final Map<String, String> postReplacements) {
        replacementsTable.removeAllItems();
        for (final Entry<String, String> entry : postReplacements.entrySet()) {

            final Item item = replacementsTable.addItem(entry);
            item.getItemProperty("regex").setValue(entry.getKey());
            item.getItemProperty("replacement").setValue(entry.getValue());
        }
        replacementsTable.sort();
    }

    @Override
    public void setConvertMessageBoardsUrls(final boolean convert) {
        convertMessageBoardsUrls.setValue(convert);
    }

    @Override
    protected void navigationTo(final String[] arguments) {

    }

    @Override
    public void showNotification(final String notification) {
        getRoot().showNotification(notification);
    }

    @Override
    public void setGoogleAnalyticsTrackerId(
            final String googleAnalyticsTrackerId) {
        final String value = (googleAnalyticsTrackerId == null) ? ""
                : googleAnalyticsTrackerId;
        analyticsTrackerIdField.setValue(value);
    }
}