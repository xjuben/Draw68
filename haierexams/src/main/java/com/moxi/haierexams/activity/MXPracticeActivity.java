package com.moxi.haierexams.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moxi.haierexams.R;
import com.moxi.haierexams.adapter.OptionAdapter;
import com.moxi.haierexams.adapter.TestExamsAdapter;
import com.moxi.haierexams.adapter.TimeTableCourseAdapter;
import com.moxi.haierexams.cache.ACache;
import com.moxi.haierexams.db.SQLUtil;
import com.moxi.haierexams.http.HttpVolleyCallback;
import com.moxi.haierexams.http.VolleyHttpUtil;
import com.moxi.haierexams.model.ChoseResultModel;
import com.moxi.haierexams.model.CourseModel;
import com.moxi.haierexams.model.HistoryModel;
import com.moxi.haierexams.model.OptionModel;
import com.moxi.haierexams.model.SyncExamsModel;
import com.moxi.haierexams.model.TuiJianSJModel;
import com.moxi.haierexams.view.TagBaseAdapter;
import com.moxi.haierexams.view.TagCloudLayout;
import com.mx.mxbase.base.BaseActivity;
import com.mx.mxbase.constant.Constant;
import com.mx.mxbase.interfaces.OnItemClickListener;
import com.mx.mxbase.utils.GsonTools;
import com.mx.mxbase.utils.Log;
import com.mx.mxbase.utils.MXUamManager;
import com.mx.mxbase.utils.Toastor;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import okhttp3.Call;


/**
 * Created by Archer on 16/8/10.
 */
public class MXPracticeActivity extends BaseActivity implements View.OnClickListener {

    @Bind(R.id.ll_base_back)
    LinearLayout llBack;
    @Bind(R.id.ll_practice_chosen)
    LinearLayout llChosen;
    @Bind(R.id.tv_base_mid_title)
    TextView tvMidTitle;
    @Bind(R.id.tv_base_back)
    TextView tvBaseBack;
    @Bind(R.id.tv_base_more)
    TextView tvBaseMore;
    @Bind(R.id.ll_base_more)
    LinearLayout llBaseMore;
    @Bind(R.id.recycler_practice_kemu)
    RecyclerView recyclerKemu;//??????
    @Bind(R.id.recycler_practice_exaggerate)
    RecyclerView recyclerExaggerate;//????????????
    @Bind(R.id.recycler_practice_tixing)
    RecyclerView recyclerTixing;//??????
    @Bind(R.id.gridview_practice_exams)
    GridView gridExams;
    @Bind(R.id.tv_practice_chosen_value)
    TextView tvChosenValue;
    @Bind(R.id.ll_practice_more)
    LinearLayout llMore;
    @Bind(R.id.tv_practice_tongbu)
    TextView tvTongBu;
    @Bind(R.id.tv_to_see_ls_exams)
    TextView tvSeeLs;
    @Bind(R.id.tv_practice_last_name)
    TextView tvLaseName;
    @Bind(R.id.tv_practice_last_state)
    TextView tvLastState;
    @Bind(R.id.tv_practice_last_date)
    TextView tvLastDate;
    @Bind(R.id.tv_practice_zh)
    TextView tvPracticeZh;
    @Bind(R.id.tag_clound_layout_jc)
    TagCloudLayout tagCloudLayout;

    private TimeTableCourseAdapter adapter;
    private List<CourseModel> listCourse = new ArrayList<>();
    //?????????
    private List<OptionModel> listCBS = new ArrayList<>();
    //?????????????????????
    private OptionAdapter txAdapter;
    private List<OptionModel> listTx = new ArrayList<>();
    //???????????????????????????
    private OptionAdapter exAdapter;
    private List<OptionModel> listEx = new ArrayList<>();
    private TagBaseAdapter adapterTag;

    public static int semId = 0;//??????id
    private String semName;
    private int secId = 0;//??????id
    private String secName;
    private int subId;//??????id
    private String subName;
    public static int pubId;//?????????id
    private String pubName;
    public static String dicId = "";//????????????id
    public static String txId = "";//??????id

    @Override
    protected int getMainContentViewId() {
        return R.layout.mx_activity_practice_new;
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        init();
        if (share.getString("chose_requirement_option_value").equals("")) {
            Intent chosen = new Intent(this, MXRequirementOptionActivity.class);
            startActivityForResult(chosen, 10001);
        } else {
            //TODO
            ChoseResultModel crm = GsonTools.getPerson(share.getString("chose_requirement_option_value"), ChoseResultModel.class);
            semId = crm.getXQID();
            secId = crm.getPeriodId();
            semName = crm.getXQDesc();
            secName = crm.getPeroidDesc();
            tvChosenValue.setText(crm.getChoseValue());
            loadData();
        }
    }

    private void loadData() {
        listCourse.clear();
        if (semId == 0)
            return;
        //??????
        listCourse = SQLUtil.getInstance(this).getKeMuFromDb(String.valueOf(semId));
        if (listCourse != null && !listCourse.isEmpty()) {
            listCourse.get(0).setChosen(true);
            subId = listCourse.get(0).getId();
            subName = listCourse.get(0).getCourseName();
        }
        adapter.setData(listCourse);
        adapter.notifyDataSetChanged();

        if (listCourse != null && listCourse.size() > 0) {
            String subjectId = listCourse.get(0).getId() + "";
            loadTxData(subjectId);
            loadCBSData(subjectId);
        }
    }

    private void loadTxData(String subjectId) {
        //??????
        listTx = SQLUtil.getInstance(this).getTXFromDb(subjectId, secId + "");
        txAdapter.setData(listTx);
        txAdapter.notifyDataSetChanged();
    }

    private void loadCBSData(String subId) {
        //?????????
        listCBS = SQLUtil.getInstance(this).getCBSFromDb(secId + "", subId);
        if (listCBS != null && !listCBS.isEmpty()) {
            listCBS.get(0).setChosen(true);
            this.pubId = listCBS.get(0).getId();
            this.pubName = listCBS.get(0).getOptionName();
        }
        adapterTag.setData(listCBS);
        adapterTag.notifyDataSetChanged();
    }


    /**
     * ???????????????
     */
    private void init() {
        llBack.setVisibility(View.VISIBLE);
        tvBaseBack.setText("??????");
        llBaseMore.setVisibility(View.VISIBLE);
        tvBaseMore.setText("???????????????");
        tvMidTitle.setText("???????????????");
        //??????????????????
        llBack.setOnClickListener(this);
        llChosen.setOnClickListener(this);
        llMore.setOnClickListener(this);
        tvTongBu.setOnClickListener(this);
        tvSeeLs.setOnClickListener(this);
        llBaseMore.setOnClickListener(this);
        tvPracticeZh.setOnClickListener(this);
        tvChosenValue.setText("???????????????????????????");

        recyclerKemu.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new TimeTableCourseAdapter(this, listCourse);
        recyclerKemu.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                for (int i = 0; i < listCourse.size(); i++) {
                    if (i == position) {
                        listCourse.get(i).setChosen(true);
                        subId = listCourse.get(i).getId();
                        subName = listCourse.get(i).getCourseName();
                        loadTxData(subId + "");
                        loadCBSData(subId + "");
                    } else {
                        listCourse.get(i).setChosen(false);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        //??????
        LinearLayoutManager llManager = new LinearLayoutManager(this);
        llManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerTixing.setLayoutManager(llManager);
        setTxAdapter();
        //????????????
        LinearLayoutManager l2Manager = new LinearLayoutManager(this);
        l2Manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerExaggerate.setLayoutManager(l2Manager);
        //
        setExAdapter();
        //????????????
        setJcAbout();
    }

    /**
     * ????????????
     */
    private void setJcAbout() {
        adapterTag = new TagBaseAdapter(this, listCBS);
        adapterTag.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = Integer.valueOf(view.getTag().toString());
                for (int i = 0; i < listCBS.size(); i++) {
                    if (i == position) {
                        listCBS.get(i).setChosen(true);
                        pubId = listCBS.get(i).getId();
                        pubName = listCBS.get(i).getOptionName();
                    } else {
                        listCBS.get(i).setChosen(false);
                    }
                }
                adapterTag.notifyDataSetChanged();
            }
        });
        tagCloudLayout.setAdapter(adapterTag);
    }

    /**
     * ??????????????????
     *
     * @param
     */
    private void setExAdapter() {
        listEx = SQLUtil.getInstance(this).getLDFromDb();
        exAdapter = new OptionAdapter(this, listEx);
        recyclerExaggerate.setAdapter(exAdapter);
        exAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                for (int i = 0; i < listEx.size(); i++) {
                    if (i == position) {
                        listEx.get(i).setChosen(true);
                        //TODO
                        dicId = listEx.get(i).getId() + "";
                    } else {
                        listEx.get(i).setChosen(false);
                    }
                    exAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
    }

    /**
     * ??????????????????
     *
     * @param
     */
    private void setTxAdapter() {
        txAdapter = new OptionAdapter(this, listTx);
        recyclerTixing.setAdapter(txAdapter);
        txAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                for (int i = 0; i < listTx.size(); i++) {
                    if (i == position) {
                        listTx.get(i).setChosen(true);
                        txId = listTx.get(i).getId() + "";
                    } else {
                        listTx.get(i).setChosen(false);
                    }
                    txAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
    }


    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        //????????????
        getHistoryData(MXUamManager.queryUser(this,false), 1);
        getExaData("");
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {

    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10001) {
            if (resultCode == 1001) {
                Bundle b = data.getExtras();
                if (b == null) {
                    return;
                }
                String str = b.getString("tv_chosen_value");
                semId = b.getInt("chosenXqId", 0);
                secId = b.getInt("chosenPeriodId", 0);
                semName = b.getString("chosenXq");
                secName = b.getString("chosenPeriod");
                tvChosenValue.setText(str);
                loadData();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_base_back:
                this.finish();
                break;
            case R.id.ll_practice_chosen:
                Intent chosen = new Intent(this, MXRequirementOptionActivity.class);
                startActivityForResult(chosen, 10001);
                break;
            case R.id.ll_practice_more:
                Log.d("ex", "tvChosenValue==>" + tvChosenValue.getText().toString());
                Intent intent = new Intent(this, MXExamsListActivity.class);
                intent.putExtra("secId", secId);
                intent.putExtra("semId", semId);
                startActivity(intent);
                break;
            case R.id.tv_practice_tongbu:
                if (checkCoseck()) {
                    SyncExamsModel sem = new SyncExamsModel();
                    Intent intent1 = new Intent(this, SynchronousActivity.class);
                    sem.setCob_pub_id(pubId + "");
                    sem.setCob_pub_name(pubName);
                    sem.setCob_sec_id(secId + "");
                    sem.setCob_sec_name(secName);
                    sem.setCob_sub_id(subId + "");
                    sem.setCob_sub_name(subName);
                    sem.setCoc_sem_name(semName);
                    sem.setCos_sem_id(semId + "");
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("sem_info", sem);
                    intent1.putExtras(bundle);
                    startActivity(intent1);
                }
                break;
            case R.id.tv_practice_zh:
                if (checkCoseck()) {
                    Intent aaa = new Intent(this, MXExamsListActivity.class);
                    aaa.putExtra("secId", secId);
                    aaa.putExtra("semId", semId);
                    startActivity(aaa);
                }
                break;
            case R.id.tv_to_see_ls_exams:
                startActivity(new Intent(this, MXHistoryActivity.class).putExtra("sub_id", ""));
                break;
            case R.id.ll_base_more:
                startActivity(new Intent(this, WrongSubjectActivity.class));
                break;
            default:
                break;
        }
    }

    private boolean checkCoseck() {
        if (semId == 0) {
            Toastor.showToast(this, "???????????????");
            return false;
        }
        if (secId == 0) {
            Toastor.showToast(this, "???????????????");
            return false;
        }
        if (subId == 0) {
            Toastor.showToast(this, "???????????????");
            return false;
        }
        if (pubId == 0) {
            Toastor.showToast(this, "??????????????????");
            return false;
        }
        return true;
    }

    /**
     * ??????????????????
     *
     * @param kemuid ??????id
     */
    private void getExaData(String kemuid) {
        OkHttpUtils.post().url(Constant.HISTORDISS).addParams("appSession", MXUamManager.queryUser(this,false)).addParams("rows", "2000").
                addParams("subId", kemuid).addParams("secId", secId + "").build().connTimeOut(10000).execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                if (MXPracticeActivity.this.isfinish)return;
                String temp = ACache.get(MXPracticeActivity.this).getAsString(Constant.HISTORDISS);
                if (temp != null) {
                    showSj(temp);
                } else {
                    Toastor.showToast(MXPracticeActivity.this, "????????????????????????");
                }
            }

            @Override
            public void onResponse(String response, int id) {
                if (MXPracticeActivity.this.isfinish)return;
                try {
                    ACache.get(MXPracticeActivity.this).put(Constant.HISTORDISS, response);
                    showSj(response);
                }catch (Exception e){}

            }
        });
    }

    /**
     * ??????????????????
     *
     * @param response ?????????json????????????
     */
    private void showSj(String response) {
        TuiJianSJModel tjsj = GsonTools.getPerson(response, TuiJianSJModel.class);
        final List<HashMap<String, Object>> result = new ArrayList<>();
        if (tjsj != null) {
            for (TuiJianSJModel.Paper paper : tjsj.getResult().getList()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("pap_id", paper.getId());
                hashMap.put("pap_name", paper.getName());
                hashMap.put("pap_sub_name", paper.getSubName());
                hashMap.put("pap_sub_id", paper.getSubId());
                hashMap.put("pap_is_done", paper.getCprId());
                result.add(hashMap);
            }
            //Todo
            TestExamsAdapter exaAdapter = new TestExamsAdapter(MXPracticeActivity.this, result);
            gridExams.setAdapter(exaAdapter);
            gridExams.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    Intent exams = new Intent(MXPracticeActivity.this, ExamsTestActivity.class);
                    exams.putExtra("test_pap_id", result.get(position).get("pap_id").toString());
                    exams.putExtra("test_pap_name", result.get(position).get("pap_name").toString());
                    exams.putExtra("test_pap_cprId", result.get(position).get("pap_is_done").toString());
                    startActivity(exams);
                }
            });
        } else {
            Toastor.showToast(MXPracticeActivity.this, tjsj.getMsg());
        }
    }

    /**
     * ????????????????????????
     *
     * @param appSession ??????session
     * @param page       ??????
     */
    private void getHistoryData(String appSession, int page) {
        HashMap<String, String> param = new HashMap<>();
        param.put("page", page + "");
        param.put("rows", 15 + "");
        param.put("subId", "");
        param.put("appSession", appSession);
        VolleyHttpUtil.post(this, Constant.HISTORYURL, param, new HttpVolleyCallback() {
            @Override
            public void onSuccess(String data) {
                ACache.get(MXPracticeActivity.this).put(Constant.HISTORYURL, data);
                List<HistoryModel> historyModels = parseHistoryData(data);
                if (historyModels.size() == 1) {
                    String title = historyModels.get(0).getTitle();
                    String time = historyModels.get(0).getCreatetime();
                    tvLaseName.setText(title);
                    tvLastDate.setText(time);
                }
            }

            @Override
            public void onFilad(String msg) {
                String temp = ACache.get(MXPracticeActivity.this).getAsString(Constant.HISTORYURL);
                if (temp != null) {
                    List<HistoryModel> historyModels = parseHistoryData(temp);
                    if (historyModels.size() == 1) {
                        String title = historyModels.get(0).getTitle();
                        String time = historyModels.get(0).getCreatetime();
                        tvLaseName.setText(title);
                        tvLastDate.setText(time);
                    }
                }
            }
        });

    }

    private List<HistoryModel> parseHistoryData(String data) {
        List<HistoryModel> dataList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(data);
            int code = jsonObject.optInt("code", -1);
            if (code == 0) {
                JSONObject jsonObject1 = jsonObject.optJSONObject("result");
                JSONArray jsonArray = jsonObject1.optJSONArray("list");
                HistoryModel historyModel;
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (i >= 1) {
                        break;
                    }
                    historyModel = new HistoryModel();
                    JSONObject liObj = jsonArray.getJSONObject(i);
                    historyModel.setTitle(liObj.optString("title"));
                    historyModel.setCreatetime(liObj.optString("createtime"));
                    historyModel.setId(liObj.optInt("id"));
                    historyModel.setMemId(liObj.optInt("memId"));
                    historyModel.setType(liObj.optInt("type"));
                    dataList.add(historyModel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataList;
    }
}
