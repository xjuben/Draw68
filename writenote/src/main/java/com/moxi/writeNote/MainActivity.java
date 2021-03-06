package com.moxi.writeNote;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.moxi.handwritinglibs.asy.DeleteFilesAsy;
import com.moxi.handwritinglibs.asy.ExportFileAsy;
import com.moxi.handwritinglibs.db.WritPadModel;
import com.moxi.handwritinglibs.db.WritePadUtils;
import com.moxi.handwritinglibs.listener.DeleteListener;
import com.moxi.handwritinglibs.listener.ExportListener;
import com.moxi.handwritinglibs.model.ExtendModel;
import com.moxi.handwritinglibs.utils.DbPhotoLoader;
import com.moxi.writeNote.Activity.NewActivity;
import com.moxi.writeNote.Activity.SettingActivity;
import com.moxi.writeNote.Model.FloderInformation;
import com.moxi.writeNote.Model.SimpleWriteModel;
import com.moxi.writeNote.adapter.WriteItemAdapter;
import com.moxi.writeNote.config.ActivityUtils;
import com.moxi.writeNote.config.ConfigInfor;
import com.moxi.writeNote.config.ConfigerUtils;
import com.moxi.writeNote.listener.PasteListener;
import com.moxi.writeNote.popwin.SortStylePopwin;
import com.moxi.writeNote.share.ContentBuilderInterface;
import com.moxi.writeNote.share.ContentUtils;
import com.moxi.writeNote.sortUtils.SortName;
import com.moxi.writeNote.utils.MoveFileConfig;
import com.moxi.writeNote.utils.PDFCreateRunalbe;
import com.moxi.writeNote.utils.PasteAsy;
import com.moxi.writeNote.utils.UserInformation;
import com.mx.mxbase.base.MyApplication;
import com.mx.mxbase.constant.APPLog;
import com.mx.mxbase.dialog.InputDialog;
import com.mx.mxbase.dialog.ListDialog;
import com.mx.mxbase.interfaces.InsureOrQuitListener;
import com.mx.mxbase.utils.FileUtils;
import com.mx.mxbase.utils.StringUtils;
import com.mx.mxbase.utils.ToastUtils;
import com.mx.mxbase.view.SildeFrameLayout;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;


public class MainActivity extends WriteBaseActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    /**
     * ??????????????????
     */
    private List<FloderInformation> informations = new ArrayList<FloderInformation>();
    /**
     * ???????????????????????????
     */
    private String floder = "";
    /**
     * ??????????????????
     */
    private int pageIndex = 0;
    /**
     * ??????????????????
     */
    private final int pageSize = 12;
    /**
     * ???????????????
     */
    private int totalPage;
    /**
     * ?????????????????????models
     */
    private List<WritPadModel> writPadModels = new ArrayList<WritPadModel>();
    /**
     * ????????????model
     */
    private List<WritPadModel> middleModels = new ArrayList<WritPadModel>();
    /**
     * ?????????adapter
     */
    private WriteItemAdapter adapter;

    //    ???????????????
    @Bind(R.id.show_title)
    TextView show_title;
    @Bind(R.id.page_status)
    TextView page_status;
    @Bind(R.id.setting)
    TextView setting;
    @Bind(R.id.new_marks)
    TextView new_marks;
    @Bind(R.id.select)
    TextView select;
    @Bind(R.id.sort)
    TextView sort;

    @Bind(R.id.silde_layout)
    SildeFrameLayout silde_layout;
    @Bind(R.id.write_item)
    GridView write_item;

    @Bind(R.id.paste_file)
    TextView paste;
    @Bind(R.id.selec_control)
    LinearLayout selec_control;
    @Bind(R.id.delete)
    TextView delete;
    @Bind(R.id.move)
    TextView move;
    @Bind(R.id.last_page)
    ImageButton last_page;
    @Bind(R.id.show_index)
    TextView show_index;
    @Bind(R.id.next_page)
    ImageButton next_page;
    private HomeKeyBrodcast homeKeyBrodcast = new HomeKeyBrodcast();
    private SortStylePopwin sortStylePopwin;
    private String flag_version_stu;
    private boolean synchrodata=false;//??????sd??????????????????????????????

    @Override
    protected int getMainContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            informations.clear();
            List<FloderInformation> infors = (List<FloderInformation>) savedInstanceState.getSerializable("informations");
            if (infors == null) {
                android.os.Process.killProcess(android.os.Process.myPid());
                return;
            }
            flag_version_stu = savedInstanceState.getString("flag_version_stu");
            informations.addAll(infors);
        } else {
            flag_version_stu = getIntent().getStringExtra("flag_version_stu");
        }
        UserInformation.getInstance().setFlag(flag_version_stu);

        registerReceiver(homeKeyBrodcast, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        initView();

        ActivityUtils.getInstance().addActivity(this);
    }

    private void initView() {
        show_title.setOnClickListener(this);
        setting.setOnClickListener(this);
        new_marks.setOnClickListener(this);
        select.setOnClickListener(this);

        write_item.setOnItemClickListener(this);
        write_item.setOnItemLongClickListener(this);

        paste.setOnClickListener(this);
        delete.setOnClickListener(this);
        move.setOnClickListener(this);
        last_page.setOnClickListener(this);
        next_page.setOnClickListener(this);
        sort.setOnClickListener(this);
        silde_layout.setListener(sildeEventListener);

        sortStylePopwin = new SortStylePopwin(this, share.getInt("sortType"), new SortStylePopwin.SelectSortListener() {
            @Override
            public void onUpdate(int style) {
                share.setCache("sortType", style);
                sortType(true);
            }
        });
    }
//??????db??????

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_title:
                onBackPressed();
                break;
            case R.id.setting://??????
                if (isSelect()) {
                    changeSelect();
                    return;
                }
                if (isMove()) {
                    changeMove();
                    return;
                }
                startActivityForResult(new Intent(this, SettingActivity.class), 10);
                break;
            case R.id.new_marks:
                if (isSelect()) {
                    changeSelect();
                    return;
                }
                if (isMove()) {
                    changeMove();
                    return;
                }
                inputDialog = InputDialog.getdialog(this, getString(R.string.new_floder), ConfigerUtils.hitnInput, new InputDialog.InputListener() {
                    @Override
                    public void quit() {
                    }

                    @Override
                    public void insure(String name) {
                        if (name.equals("")) {
                            return;
                        }
                        if (ConfigerUtils.isFail(name)) return;
                        WritPadModel writPadModel = WritePadUtils.getInstance().isSavedWrite(getFloder(name), 0);
                        if (writPadModel != null && writPadModel.name.equals(name)) {
                            ToastUtils.getInstance().showToastShort("?????????????????????");
                            return;
                        }
                        if (WritePadUtils.getInstance().isNameSavedWrite(name, floder, 0)) {
                            ToastUtils.getInstance().showToastShort("?????????????????????");
                            return;
                        }
                        /**?????????????????????*/
                        String saveCode = getFloder(writPadModel == null ? name : name + System.currentTimeMillis());

                        WritPadModel model = new WritPadModel(name, saveCode, 0, floder, 0, "");
                        WritePadUtils.getInstance().saveData(model);
                        //????????????
                        initPageInformation();
                    }
                });
                break;
            case R.id.sort:
                sortStylePopwin.popWindow(select);
                break;
            case R.id.select:
                if (isMove()) {
                    changeMove();
                    return;
                }
                changeSelect();
                break;
            case R.id.paste_file://??????
                if (MoveFileConfig.getInstance().getParentCode() != null && MoveFileConfig.getInstance().getParentCode().equals(floder)) {
                    ToastUtils.getInstance().showToastShort("????????????????????????");
                    return;
                }
                dialogShowOrHide(true, "?????????...");
                new PasteAsy(floder, new PasteListener() {
                    @Override
                    public void onPaste(boolean is, String hitn) {
                        if (isfinish) return;
                        dialogShowOrHide(false, "");
                        changeMove();
                        if (!is) {
                            insureDialog(hitn, "??????", null);
                        } else {
                            ToastUtils.getInstance().showToastShort("????????????");
                        }
                        initPageInformation();
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case R.id.delete:
                if (adapter.getIsSelectWritePads().size() == 0) {
                    ToastUtils.getInstance().showToastShort("?????????????????????");
                    return;
                }
                //?????????????????????
                deleteFiles(adapter.getIsSelectWritePads());
                break;
            case R.id.move:
                if (adapter.getIsSelectWritePads().size() == 0) {
                    ToastUtils.getInstance().showToastShort("??????????????????");
                    return;
                }
                List<SimpleWriteModel> commonModels = new ArrayList<SimpleWriteModel>();
                for (int i = 0; i < adapter.getIsSelectWritePads().size(); i++) {
                    WritPadModel model1 = adapter.getIsSelectWritePads().get(i);
                    SimpleWriteModel simple = new SimpleWriteModel();
                    simple.id = model1.id;
                    simple.name = model1.name;
                    simple.isFloder = model1.isFolder;
                    simple.saveCode = model1.saveCode;
                    simple.parentCode = model1.parentCode;
                    commonModels.add(simple);
                }
                MoveFileConfig.getInstance().init(commonModels, true);
                changeSelect();
                changeMove();
                break;
            case R.id.last_page:
                moveLeft();
                break;
            case R.id.next_page:
                moveRight();
                break;
            default:
                break;
        }
    }

    /**
     * ??????????????????
     *
     * @param isSelects
     */
    private void deleteFiles(final List<WritPadModel> isSelects) {
        insureDialog("???????????????????????????", "????????????", new InsureOrQuitListener() {
            @Override
            public void isInsure(Object code, final boolean is) {
                if (is) {
                    dialogShowOrHide(true, "???????????????...");
                    new DeleteFilesAsy(isSelects, new DeleteListener() {
                        @Override
                        public void onDelete(boolean isDelete) {
                            if (isfinish) return;
                            dialogShowOrHide(false, "???????????????...");
                            if (isDelete) {
                                if (isSelect()) {
                                    changeSelect();
                                } else {
                                    adapter.setShowAelect(false, null);
                                }
                                share.setCache("noData",true);
                                initPageInformation();
//                                ToastUtils.getInstance().showToastShort("????????????");
                            } else {
                                ToastUtils.getInstance().showToastShort("????????????");
                            }
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //??????????????????
        DbPhotoLoader.getInstance().isStopPool(true);
        WritPadModel model = middleModels.get(position);
        if (isSelect()) {
            if (model.id == -1) return;
            //??????????????????
            adapter.setIsSelectWritePads(model);
            adapter.updateSelect(position, write_item);
            return;
        }
        //????????????/????????????
        if ((model.isFolder == -1 || model.isFolder == 1) && !isMove()) {
//            isToNoteActivity = true;
            if (model.isFolder == -1) {
                //??????????????????????????????
                if (!internalJuge()) {
                    showToast("???????????????????????????????????????");
                    return;
                }
            }

            startWriteNoteActivity(model);
        } else if (model.isFolder == 0) {
            //?????????
            informations.add(new FloderInformation(model.saveCode, model.name, 0));
            WritePadUtils.getInstance().upDateTime(model.id);
            initPageInformation();
        }
    }

    private long startWriteNoteTime = 0;

    private void startWriteNoteActivity(WritPadModel model) {
        if (startWriteNoteTime == 0 || (System.currentTimeMillis() - startWriteNoteTime) > 1000) {
            Intent intent = new Intent(MainActivity.this, NewActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("model", model);
            intent.putExtras(bundle);
            startActivity(intent);
            startWriteNoteTime = System.currentTimeMillis();
//            startActivity(new Intent(this, NewActivity.class));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (isSelect() || isMove()) return true;
        final WritPadModel model = middleModels.get(position);
        if (model.isFolder == -1) {
            //????????????
        } else if (model.isFolder == 0) {
            //??????????????????
            reName(model);
        } else if (model.isFolder == 1) {
            //???????????????????????????
            showManagerOneFilePopupWindow(view, model, position);
        }
        return true;
    }

    private InputDialog inputDialog;

    /**
     * ?????????
     *
     * @param model
     */
    private void reName(final WritPadModel model) {
        inputDialog = InputDialog.getdialog(this, getString(R.string.re_name), ConfigerUtils.hitnInput, new InputDialog.InputListener() {
            @Override
            public void quit() {
            }

            @Override
            public void insure(String name) {
                if (name.equals("") || name.equals(model.name)) {
                    return;
                }
                if (ConfigerUtils.isFail(name)) return;

                WritPadModel writPadModel = WritePadUtils.getInstance().isSavedWrite(getFloder(name), model.isFolder);
                if (writPadModel != null && writPadModel.name.equals(name)) {
                    ToastUtils.getInstance().showToastShort("??????????????????");
                    return;
                }
                if (WritePadUtils.getInstance().isNameSavedWrite(name, floder, model.isFolder)) {
                    ToastUtils.getInstance().showToastShort("??????????????????");
                    return;
                }
                /**?????????????????????*/
                WritePadUtils.getInstance().upDateName(name, model.id);
//                if (model.isFolder==1){
//                    DrawIndexUtils.getInstance().updateSaveCode(model.saveCode,getFloder(name));
//                }
                //????????????
                initPageInformation();
            }
        });
    }

    /**
     * ??????????????????????????????
     */
    private void initPageInformation() {
        if (informations.size() == 0) {
            if (UserInformation.getInstance().isHidePattern()) {
                //??????????????????,????????????????????????????????????
                informations.add(new FloderInformation(UserInformation.getInstance().getUserPassword(), "??????", 0));
            } else {
                //???????????????
                informations.add(new FloderInformation(ConfigInfor.rootDir, "??????", 0));
            }
        }
        FloderInformation information = informations.get(informations.size() - 1);
        floder = information.floder;
        pageIndex = information.pageIndex;
        show_title.setText(information.name);
        sortType(false);
    }

    private void sortType(boolean setSort) {

        int sType = share.getInt("sortType");
        if (sType == 0) {
            writPadModels.clear();
            writPadModels.addAll(WritePadUtils.getInstance().getListMirks(floder));
        } else {//???????????????
            if (!setSort) {
                writPadModels.clear();
                writPadModels.addAll(WritePadUtils.getInstance().getListMirks(floder));
            }
            Collections.sort(writPadModels, new SortName());
        }
        if (writPadModels.size()<=1&&!synchrodata){
            //????????????????????????????????????
            if (!share.getBoolean("noData")) {
                saveDrawDb(false);
            }
        }
        synchrodata=true;

        changePage();
    }

    /**
     * ????????????
     */
    private void changePage() {
        int size = writPadModels.size();
        totalPage = (size / pageSize) + ((size % pageSize == 0) ? 0 : 1);
        if (totalPage <= pageIndex) {
            pageIndex = totalPage - 1;
        }
        initAdapter();
    }
private long initAdapterTime=0;

    /**
     * ?????????adapter
     */
    private void initAdapter() {
        getHandler().removeMessages(100);
        if (System.currentTimeMillis()-initAdapterTime<1000){
            getHandler().sendEmptyMessageDelayed(100,1000);
        }else {
            //??????????????????????????????
            informations.get(informations.size() - 1).pageIndex = pageIndex;
            middleModels.clear();
            if (totalPage > 0 && pageIndex >= 0) {
                if ((totalPage - 1) > pageIndex) {
                    middleModels.addAll(writPadModels.subList(pageSize * pageIndex, pageSize * (pageIndex + 1)));
                } else {
                    middleModels.addAll(writPadModels.subList(pageSize * pageIndex, writPadModels.size()));
                }
            }
            if (adapter == null) {
                adapter = new WriteItemAdapter(this, middleModels);
                write_item.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
        show_index.setText(String.valueOf(pageIndex + 1) + "/" + String.valueOf(totalPage));
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        initPageInformation();
        if (!internalJuge()) {
            showToast("???????????????????????????????????????????????????????????????????????????????????????????????????");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState ) {
        outState.putSerializable("informations", (Serializable) informations);
        outState.putString("flag_version_stu", flag_version_stu);
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        ActivityUtils.getInstance().ClearActivity(this);
        unregisterReceiver(homeKeyBrodcast);
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (isSelect()) {
            changeSelect();
            return;
        }
        if (informations == null || informations.size() <= 1) {
//            android.os.Process.killProcess(android.os.Process.myPid());
            share.setCache("noData",writPadModels.size()<=1);
            saveDrawDb(true);
            this.finish();
        } else {
            informations.remove(informations.size() - 1);
            initPageInformation();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            //?????????
            moveLeft();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            //?????????
            moveRight();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void moveRight() {
        if (pageIndex >= totalPage - 1) {
            ToastUtils.getInstance().showToastShort("?????????????????????");
            return;
        } else {
            pageIndex++;
            initAdapter();
        }
    }

    private void moveLeft() {

        if (pageIndex > 0 && (pageIndex <= totalPage - 1)) {
            pageIndex--;
            initAdapter();
        } else {
            ToastUtils.getInstance().showToastShort("??????????????????");
        }
    }

    private void changeSelect() {
        if (isSelect()) {
            adapter.setShowAelect(false, write_item);
            selec_control.setVisibility(View.INVISIBLE);
        } else {
            adapter.setShowAelect(true, write_item);
            selec_control.setVisibility(View.VISIBLE);
        }
    }

    private void changeMove() {
        if (isMove()) {
            paste.setVisibility(View.GONE);
            page_status.setText("");
            adapter.setShowAelect(false, write_item);
//            adapter.updateAllSelect(write_item);
            MoveFileConfig.getInstance().ClearMove();
        } else {
            paste.setVisibility(View.VISIBLE);
            page_status.setText("????????????");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MoveFileConfig.getInstance().ClearMove();
        if (isMove()) {
            changeMove();
        }
        dialogShowOrHide(false, "");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sortStylePopwin != null)
            sortStylePopwin.dismissSort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserInformation.getInstance().clear();
    }

    private boolean isSelect() {
        if (adapter == null) return false;
        return adapter.isShowAelect();
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private boolean isMove() {
        return paste.getVisibility() == View.VISIBLE;
    }

    private String getFloder(String name) {
        return floder + "/" + name;
    }

    private SildeFrameLayout.SildeEventListener sildeEventListener = new SildeFrameLayout.SildeEventListener() {
        @Override
        public void onSildeEventLeft() {
            moveLeft();
        }

        @Override
        public void onSildeEventRight() {
            moveRight();
        }
    };

    private PopupWindow onefile;

    /**
     * ??????????????????pop
     *
     * @param view
     */
    public void showManagerOneFilePopupWindow(View view, WritPadModel model, int position) {
        // ????????????????????????????????????????????????
        View contentView = LayoutInflater.from(this).inflate(
                R.layout.pop_one_file_manager, null);
        // ???????????????????????????

        TextView export_pdf = (TextView) contentView.findViewById(R.id.export_pdf);
        TextView export_photo = (TextView) contentView.findViewById(R.id.export_photo);
        TextView share_photo = (TextView) contentView.findViewById(R.id.share_photo);
        TextView delete = (TextView) contentView.findViewById(R.id.delete);
        TextView rename = (TextView) contentView.findViewById(R.id.rename);
        TextView setting_page = (TextView) contentView.findViewById(R.id.setting_page);

        onefile = new PopupWindow(contentView,
                MyApplication.dip2px(160), LinearLayout.LayoutParams.WRAP_CONTENT, true);

        onefile.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    onefile.dismiss();
                    onefile = null;
                    return true;
                }
                return false;
            }
        });
        String valuePage = model.getExtendModel().noAllPageReplaceStyle == 0 ? "??????" : "??????";
        setting_page.setText(getResources().getText(R.string.setting_page_stype) + valuePage);

        export_pdf.setTag(position);
        export_photo.setTag(position);
        share_photo.setTag(position);
        rename.setTag(position);
        delete.setTag(position);

        setting_page.setTag(position);

        export_pdf.setOnClickListener(onfile);
        export_photo.setOnClickListener(onfile);
        share_photo.setOnClickListener(onfile);
        delete.setOnClickListener(onfile);
        rename.setOnClickListener(onfile);
        setting_page.setOnClickListener(onfile);
        // ????????????????????????show
        onefile.setBackgroundDrawable(new BitmapDrawable());

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        if (location[0] + view.getWidth() + 100 > MyApplication.ScreenWidth) {
            //??????
            onefile.showAtLocation(view, Gravity.NO_GRAVITY, location[0] - onefile.getWidth(), location[1]);
        } else {
            //??????
            onefile.showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.getWidth(), location[1]);
        }


    }

    View.OnClickListener onfile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position =  Integer.parseInt(v.getTag().toString());
            WritPadModel model = middleModels.get(position);
            switch (v.getId()) {
                case R.id.share_photo://?????????????????????
                case R.id.export_pdf://??????pdf??????
                    if (!StringUtils.haveSD(80)) {
                        String pdfPath = getPDFExportPath();
                        if (pdfPath.equals("")) {
                            return;
                        }
                        File file = new File(pdfPath);
                        if (file == null) return;
                        String name = getNoName(file.getAbsolutePath(), String.valueOf(System.currentTimeMillis()));
                        exportFile(file, name, model, true,v.getId()==R.id.share_photo);
                    }
                    break;
                case R.id.export_photo://????????????
                    if (!StringUtils.haveSD(80)) {
                        String photoPath = getPhotoExportPath();
                        if (photoPath.equals("")) {
                            return;
                        }
                        File file = new File(photoPath);
                        if (file == null) return;
                        exportFile(file, model.name, model, false,false);
                    }
                    break;
                case R.id.rename://?????????
                    reName(model);
                    break;
                case R.id.delete://??????
                    List<WritPadModel> isSelect = new ArrayList<WritPadModel>();
                    isSelect.add(model);
                    deleteFiles(isSelect);
                    break;
                case R.id.setting_page://????????????
                    ExtendModel extendModel = new ExtendModel(model.getExtendModel().background, model.getExtendModel().encryptPass);
                    if (model.getExtendModel().noAllPageReplaceStyle == 0) {
                        extendModel.noAllPageReplaceStyle = 1;
                    } else {
                        extendModel.noAllPageReplaceStyle = 0;
                    }
                    long _id = WritePadUtils.getInstance().judgeDataExist(model.saveCode, model.isFolder, 0);
                    if (_id != -1) {
                        //??????????????????
                        model.setExtend(extendModel);
                        WritePadUtils.getInstance().upDateExtend(model.extend, _id);
                    }
                    break;
                default:
                    break;
            }
            onefile.dismiss();
            onefile = null;
        }
    };

    /**
     * ????????????????????????????????????????????????
     *
     * @param file ?????????????????????
     * @param name ????????????
     * @return ??????????????????
     */
    private boolean isExist(File file, String name) {
        File file1 = new File(file, name);
        return file1.exists();
    }

    public void exportFile(final File file, String name, final WritPadModel model, final boolean isExportPdf, final boolean isShare) {
        boolean is = ConfigerUtils.isFail(name);
        if (isExist(file, name) || is) {
            if (!is)
                ToastUtils.getInstance().showToastShort("???????????????????????????????????????");
            //????????????
            inputDialog = InputDialog.getdialog(this, getString(R.string.re_name), "???????????????????????????", new InputDialog.InputListener() {
                @Override
                public void quit() {
                }

                @Override
                public void insure(String name) {
                    exportFile(file, name, model, isExportPdf,isShare);
                }
            });
        } else {
            //????????????
            final String filePath = (new File(file, name)).getAbsolutePath();
            dialogShowOrHide(true, "???????????????...", 10);
            new ExportFileAsy(this, model, filePath, new ExportListener() {
                @Override
                public void onExport(boolean is) {
                    if (isfinish) return;

                    if (!isExportPdf || !is) {
                        dialogShowOrHide(false, "");
                        String dir = (new File(getPDFExportPath())).getName();
                        ToastUtils.getInstance().showToastShort(is ? "???????????????" + dir + "??????" : "????????????");
                    }

                    if (is && isExportPdf) {
                        if (isShare) {
                            dialogShowOrHide(false, "");
                            selectShare(model.name,filePath);
                        } else {
                            //??????pdf??????
//                        dialogShowOrHide(true, "pdf???????????????...",15);
                            new Thread(new PDFCreateRunalbe(filePath, getPdfPath(file, model.name, 0), true, new PDFCreateRunalbe.PDFCreateListener() {
                                @Override
                                public void onFinish() {
                                    if (isfinish) return;
                                    dialogShowOrHide(false, "");
                                    String dir = (new File(getPDFExportPath())).getName();
                                    ToastUtils.getInstance().showToastShort("Pdf???????????????" + dir + "??????");
                                }

                                @Override
                                public void onFail(String msg) {
                                    if (isfinish) return;
                                    dialogShowOrHide(false, "");
                                    ToastUtils.getInstance().showToastShort(msg);
                                }

                                @Override
                                public void onProgressHitn(String hitn) {
                                    if (isfinish) return;
                                    dialogText(hitn);
                                }
                            })).start();

                        }
                    }
                }

                @Override
                public void onExportHitn(String hitn) {
                    dialogText(hitn);
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void selectShare(final String title, final String path) {

        ListDialog.getdialog(this, "???????????????????????????", new ListDialog.ClickListItemListener() {
            @Override
            public void onClickItem(final int position) {
                ContentBuilderInterface content = null;
                //??????????????????
//                if (position == 0) {//????????????
                    Bundle bundle = new Bundle();
                    bundle.putString("title", title);
                    bundle.putString("content", path);
                    bundle.putString("noteBook", ContentUtils.NoteShareDescribe);
                    bundle.putInt("shareType", 2);
                    bundle.putInt("sdkType", position + 1);

                    Intent intent = new Intent();
                    ComponentName cnInput = new ComponentName("com.moxi.biji", "com.moxi.biji.BijiActivity");
                    intent.setComponent(cnInput);
//                                Intent intent=new Intent("com.moxi.biji.start");
                    intent.putExtras(bundle);
                    startActivity(intent);
//                } else {//????????????
//                    ToastUtils.getInstance().showToastShort("????????????...");
//                }

            }
        }, "????????????", "???????????????");
    }
    public String getNoName(String floder, String name) {
        File file = new File(floder, name);
        if (file.exists()) {
            if (file.isDirectory())
                return getNoName(floder, name + "0");
        }
        return name;
    }

    public String getPdfPath(File file, String name, int index) {
        String pathname = index == 0 ? name + ".pdf" : name + index + ".pdf";
        File file1 = new File(file, pathname);
        if (file1.exists() && file1.isFile()) {
            return getPdfPath(file, name, index + 1);
        }
        return file1.getAbsolutePath();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) return;
        if (requestCode == 10) {
            //??????????????????
            informations.clear();
            initPageInformation();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case 11://??????????????????
                initPageInformation();
                break;
            case 100:
                initAdapter();
                break;
            default:
                break;
        }
    }

    private boolean isSave=false;
    /**
     * ??????db??????
     *
     * @param save ???????????????true????????????????????????false
     */
    private void saveDrawDb(final boolean save) {
        if (isSave)return;
        new Thread(){
            @Override
            public void run() {
                super.run();
                isSave=true;
                if (save) {
                    String path = getDatabasePath("writeNoteDb1.db").getPath();
                    try {
                        FileUtils.getInstance().copyFile(path, StringUtils.getSDPath(), "writeNoteDb1");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String path = StringUtils.getSDPath()+"writeNoteDb1";
                    try {
                        FileUtils.getInstance().copyFile(path, "",getDatabasePath("writeNoteDb1.db").getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getHandler().sendEmptyMessage(11);
                }
                isSave=false;
                APPLog.e("save="+save,"saveDrawDb");
            }
        }.start();

    }
}
