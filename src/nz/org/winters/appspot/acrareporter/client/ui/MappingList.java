package nz.org.winters.appspot.acrareporter.client.ui;

/*
 * Copyright 2013 Mathew Winters

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nz.org.winters.appspot.acrareporter.client.RemoteDataService;
import nz.org.winters.appspot.acrareporter.client.RemoteDataServiceAsync;
import nz.org.winters.appspot.acrareporter.shared.LoginInfo;
import nz.org.winters.appspot.acrareporter.store.MappingFileInfo;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

public class MappingList extends Composite
{
  private static UIConstants                       constants    = (UIConstants) GWT.create(UIConstants.class);
  public static final ProvidesKey<MappingFileInfo> KEY_PROVIDER = new ProvidesKey<MappingFileInfo>()
                                                                {
                                                                  @Override
                                                                  public Object getKey(MappingFileInfo item)
                                                                  {
                                                                    return item == null ? null : item.id;
                                                                  }
                                                                };

  private final class getMappingsCallback implements AsyncCallback<List<MappingFileInfo>>
  {
    @Override
    public void onSuccess(List<MappingFileInfo> result)
    {
      AppLoadingView.getInstance().stop();
      dataProvider.setList(result);
      sortHandler.setList(dataProvider.getList());
    }

    @Override
    public void onFailure(Throwable caught)
    {
      AppLoadingView.getInstance().stop();
      Window.alert(caught.getMessage());

    }
  }

  public interface DialogCallback
  {
    public void closed();
  }

  private ListHandler<MappingFileInfo>         sortHandler;
  private ListDataProvider<MappingFileInfo>    dataProvider  = new ListDataProvider<MappingFileInfo>();

  private static MappingListUiBinder           uiBinder      = GWT.create(MappingListUiBinder.class);
  @UiField
  MenuItem                                     itemDelete;
  @UiField
  MenuItem                                     itemEdit;
  @UiField
  MenuItem                                     itemUpload;
  @UiField
  Button                                       buttonClose;
  @UiField(provided = true)
  DataGrid<MappingFileInfo>                    dataGrid      = new DataGrid<MappingFileInfo>(KEY_PROVIDER);
  private DialogCallback                       callback;
  private String                               packageName;
  private final RemoteDataServiceAsync         remoteService = GWT.create(RemoteDataService.class);
  private MultiSelectionModel<MappingFileInfo> selectionModel;
  private LoginInfo                            loginInfo;

  interface MappingListUiBinder extends UiBinder<Widget, MappingList>
  {
  }

  public MappingList(LoginInfo loginInfo, String packageName, DialogCallback callback)
  {
    this.loginInfo = loginInfo;
    this.packageName = packageName;
    this.callback = callback;
    initWidget(uiBinder.createAndBindUi(this));

    sortHandler = new ListHandler<MappingFileInfo>(dataProvider.getList());
    dataGrid.addColumnSortHandler(sortHandler);
    selectionModel = new MultiSelectionModel<MappingFileInfo>(KEY_PROVIDER);
    dataGrid.setSelectionModel(selectionModel, DefaultSelectionEventManager.<MappingFileInfo> createCheckboxManager());
    initTableColumns(selectionModel, sortHandler);

    dataProvider.setList(new ArrayList<MappingFileInfo>());
    sortHandler.setList(dataProvider.getList());
    // remoteService.getMappingFiles(packageName, new getMappingsCallback());

    dataProvider.addDataDisplay(dataGrid);

    setupMenus();

    AppLoadingView.getInstance().start();

    remoteService.getMappingFiles(packageName, new getMappingsCallback());

  }

  private void initTableColumns(final SelectionModel<MappingFileInfo> selectionModel, ListHandler<MappingFileInfo> sortHandler2)
  {
    // selection check
    Column<MappingFileInfo, Boolean> checkColumn = new Column<MappingFileInfo, Boolean>(new CheckboxCell(true, false))
    {
      @Override
      public Boolean getValue(MappingFileInfo object)
      {
        // Get the value from the selection model.
        return selectionModel.isSelected(object);
      }
    };
    dataGrid.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
    dataGrid.setColumnWidth(checkColumn, 40, Unit.PX);

    // Date
    Column<MappingFileInfo, String> uploadDateColumn = new Column<MappingFileInfo, String>(new TextCell())
    {
      @Override
      public String getValue(MappingFileInfo object)
      {
        if (object.uploadDate != null)
        {
          return object.uploadDate.toString();
        } else
        {
          return "Unknown Date";
        }
      }
    };
    uploadDateColumn.setSortable(true);
    sortHandler.setComparator(uploadDateColumn, new Comparator<MappingFileInfo>()
    {
      @Override
      public int compare(MappingFileInfo o1, MappingFileInfo o2)
      {
        if (o1.uploadDate != null && o2.uploadDate != null)
          return o1.uploadDate.compareTo(o2.uploadDate);
        else
          return 0;
      }
    });
    dataGrid.addColumn(uploadDateColumn, constants.mappingListGridDate());
    dataGrid.setColumnWidth(uploadDateColumn, 200, Unit.PX);

    Column<MappingFileInfo, String> versionColumn = new Column<MappingFileInfo, String>(new TextCell())
    {
      @Override
      public String getValue(MappingFileInfo object)
      { // 2012-12-02T18:07:33.000-06:00
        return object.version;
      }
    };
    versionColumn.setSortable(true);
    sortHandler.setComparator(versionColumn, new Comparator<MappingFileInfo>()
    {
      @Override
      public int compare(MappingFileInfo o1, MappingFileInfo o2)
      {
        return o1.version.compareTo(o2.version);
      }
    });
    dataGrid.addColumn(versionColumn, constants.mappingListGridVersion());
    dataGrid.setColumnWidth(versionColumn, 100, Unit.PX);

  }

  @UiHandler("buttonClose")
  void onButtonCloseClick(ClickEvent event)
  {
    callback.closed();
  }

  public static void doDialog(final LoginInfo loginInfo, final String packageName, final DialogCallback callback)
  {

    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(constants.mappingListLabelTitle(packageName));

    MappingList mappinglist = new MappingList(loginInfo, packageName, new DialogCallback()
    {

      @Override
      public void closed()
      {
        dialogBox.hide();
        callback.closed();
      }
    });

    mappinglist.setHeight(Window.getClientHeight() - 50 + "px");

    mappinglist.setWidth("500px");
    dialogBox.setWidget(mappinglist);
    dialogBox.center();
    dialogBox.show();
  }

  void setupMenus()
  {
    itemDelete.setScheduledCommand(new Command()
    {

      @Override
      public void execute()
      {
        if (selectionModel.getSelectedSet().isEmpty())
          return;
        if (!Window.confirm(constants.mappingListConformDelete()))
          return;

        final Set<MappingFileInfo> selected = selectionModel.getSelectedSet();
        Iterator<MappingFileInfo> iter = selected.iterator();
        ArrayList<Long> ids = new ArrayList<Long>();
        while (iter.hasNext())
        {
          ids.add(iter.next().id);
        }
        AppLoadingView.getInstance().start();

        remoteService.deleteMappings(ids, new AsyncCallback<Void>()
        {

          @Override
          public void onSuccess(Void result)
          {
            remoteService.getMappingFiles(packageName, new getMappingsCallback());

          }

          @Override
          public void onFailure(Throwable caught)
          {
            AppLoadingView.getInstance().stop();

            Window.alert(caught.toString());
          }
        });
      }
    });

    itemUpload.setScheduledCommand(new Command()
    {

      @Override
      public void execute()
      {
        MappingUpload.doEditDialog(loginInfo, packageName, new MappingUpload.DialogCallback()
        {

          @Override
          public void result(boolean ok)
          {
            AppLoadingView.getInstance().start();

            remoteService.getMappingFiles(packageName, new getMappingsCallback());
          }
        });

      }
    });

    itemEdit.setScheduledCommand(new Command()
    {

      @Override
      public void execute()
      {
        if (selectionModel.getSelectedSet().isEmpty())
          return;
        final MappingFileInfo mfs = selectionModel.getSelectedSet().iterator().next();

        InputDialog.doInput(constants.mappingListLabelEditMapping(), constants.mappingListGridVersion(), mfs.version, new InputDialog.DialogCallback()
        {

          @Override
          public void result(boolean ok, String inputValue)
          {
            if (ok)
            {
              AppLoadingView.getInstance().start();

              remoteService.editMappingVersion(mfs.id, inputValue, new AsyncCallback<Void>()
              {

                @Override
                public void onSuccess(Void result)
                {
                  remoteService.getMappingFiles(packageName, new getMappingsCallback());
                }

                @Override
                public void onFailure(Throwable caught)
                {
                  AppLoadingView.getInstance().stop();

                  Window.alert(caught.toString());

                }
              });
            }

          }
        });
      }
    });

  }

}
