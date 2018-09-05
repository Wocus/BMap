package com.wocus.bmap;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.DistanceUtil;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BMapActivity extends AppCompatActivity {

    private List<LatLng> runPoints = new ArrayList<>();//绘制的大点化小点组合
    private List<BitmapDescriptor> runBitmap = new ArrayList<>();//运行中的画线的颜色

    private BaiduMap mBaiduMap;//百度地图对象

    private Timer timer;//定时器

    private MapView map; //地图

    private TextView txtName, txtSpeed, txtTime; //文字信息

    private Double mileage;//总里程

    private ImageButton btnStart, btnStop, imageButton,btnEarth;//开始停止信息

    private ProgressBar progressBar;//进度条

    private int STATE = 1;//画线状态1开始2停止

    private List<LocalBean> runLocal = new ArrayList<>();

    private int timeMin = 0;//标记隔多长时间打红点

    private boolean isDot = false;//是否需要边画线变打点

    private final int icon = R.mipmap.i2;//移动图标

    private int showType = 1;//1打点2画线3打线

    private int timeSize = 50;//播放速度50快100普通200慢

    private int STOP_STATE=0;//1按了停止   0 没按停止

    private int MAP_STATE=1;  //1普通地图  2  卫星地图

    private int LOCAL_TYPE=0;//0全部  1GPS  2WIFI   3LBS


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmap);
        map = (MapView) findViewById(R.id.mapView);
        txtName = (TextView) findViewById(R.id.txt_bmap_top_name);
        txtSpeed = (TextView) findViewById(R.id.txt_bmap_top_speed);
        txtTime = (TextView) findViewById(R.id.txt_bmap_top_time);
        btnStart = (ImageButton) findViewById(R.id.btn_bmap_start);
        btnStop = (ImageButton) findViewById(R.id.btn_bmap_stop);
        progressBar = (ProgressBar) findViewById(R.id.progressBarHorizontal);
        imageButton = (ImageButton) findViewById(R.id.imageButton2);
        btnEarth = (ImageButton) findViewById(R.id.btn_bmap_earth);
        mBaiduMap = map.getMap();
        /*mileage = DistanceUtil.getDistance(getLatLng(runLocal.get(0)), getLatLng(runLocal.get(runLocal.size() - 1)));
        txtName.setText("设备：301-005携带[20000000000005]");
        StringBuffer sb = new StringBuffer();
        sb.append("时速：");
        sb.append(runLocal.get(0).getSpd() + "KM ");//速度
        sb.append(directionType(runLocal.get(0).getDeg()));//方向
        sb.append(" 里程：" + new DecimalFormat("0.00").format(mileage / 1000) + "KM");//里程
        sb.append(" " + localType(runLocal.get(0).getSource()));//定位类型
        txtSpeed.setText(sb.toString());
        txtTime.setText("时间:" + runLocal.get(0).getUpdatetime());
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(Double.parseDouble(runLocal.get(0).getBaidu().split(",")[0]), Double.parseDouble(runLocal.get(0).getBaidu().split(",")[1]))));//定位显示到初始地点
        //定义层级*/
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(18f).build()));
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initDialog();
            }
        });
        btnEarth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MAP_STATE==1){
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                    MAP_STATE=2;
                }else{
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    MAP_STATE=1;
                }
            }
        });
    }

    TextView txtStartTime, txtEndTime, txtLocalType, txtPlayBack, txtStop, txtShow;

    private void initDialog() {
        final View view = LayoutInflater.from(this).inflate(R.layout.layout_bmap_select, null);
        txtStartTime = view.findViewById(R.id.txt_map_start_time);
        txtEndTime = view.findViewById(R.id.txt_map_end_time);
        txtLocalType = view.findViewById(R.id.txt_map_local_type);
        txtPlayBack = view.findViewById(R.id.txt_map_playback);
        txtStop = view.findViewById(R.id.txt_map_stop);
        txtShow = view.findViewById(R.id.txt_map_show);
        txtStartTime.setOnClickListener(onClickListener);
        txtEndTime.setOnClickListener(onClickListener);
        txtLocalType.setOnClickListener(onClickListener);
        txtPlayBack.setOnClickListener(onClickListener);
        txtStop.setOnClickListener(onClickListener);
        txtShow.setOnClickListener(onClickListener);
        String time=new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        txtStartTime.setText(time+" 00:00");
        txtEndTime.setText(time+" 23:59");
        new AlertDialog.Builder(BMapActivity.this)
                .setView(view)
                .setMessage("回放选项")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long count = 2000;
                        try {
                            count = getDateMin(txtStartTime.getText().toString() + ":00", txtEndTime.getText().toString() + ":00");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (count > 1440) {
                            Toast.makeText(BMapActivity.this, "相差不能大于24小时", Toast.LENGTH_LONG).show();
                        } else {
                            mBaiduMap.clear();
                            runLocal=getData();
                            if (showType == 1) {
                                initDot();
                                startDot();
                            } else if (showType == 2) {
                                isDot = false;
                                initWire();
                                startWire();
                            } else {
                                initWire();
                                isDot = true;
                                startWire();
                            }
                            mileage=0.0;
                            btnStart.performClick();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .setCancelable(false)
                .create()
                .show();

    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.txt_map_start_time: {
                    final Calendar calendar = Calendar.getInstance();
                    final int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    DatePickerDialog dialog = new DatePickerDialog(BMapActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, final int years, final int monthOfYear, final int dayOfMonth) {
                            new TimePickerDialog(BMapActivity.this,
                                    new TimePickerDialog.OnTimeSetListener() {
                                        @Override
                                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                            txtStartTime.setText(years + "-" + (monthOfYear + 1) + "-" + dayOfMonth + " " + hourOfDay + ":" + minute);
                                        }
                                    }
                                    , calendar.get(Calendar.HOUR_OF_DAY)
                                    , calendar.get(Calendar.MINUTE)
                                    , true).show();
                        }
                    }, year, month, day);
                    dialog.show();
                    break;
                }
                case R.id.txt_map_end_time: {
                    final Calendar calendar = Calendar.getInstance();
                    final int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    DatePickerDialog dialog = new DatePickerDialog(BMapActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, final int years, final int monthOfYear, final int dayOfMonth) {
                            new TimePickerDialog(BMapActivity.this,
                                    new TimePickerDialog.OnTimeSetListener() {
                                        @Override
                                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                            txtEndTime.setText(years + "-" + (monthOfYear + 1) + "-" + dayOfMonth + " " + hourOfDay + ":" + minute);
                                        }
                                    }
                                    , calendar.get(Calendar.HOUR_OF_DAY)
                                    , calendar.get(Calendar.MINUTE)
                                    , true).show();
                        }
                    }, year, month, day);
                    dialog.show();
                    break;
                }
                case R.id.txt_map_local_type: {
                    final String[] item = {"全部", "GPS", "WIFI", "LBS"};
                    new AlertDialog.Builder(BMapActivity.this)
                            .setTitle("定位类别")
                            .setItems(item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    txtLocalType.setText(item[which]);
                                    LOCAL_TYPE=which;
                                }
                            })
                            .create()
                            .show();
                    break;
                }
                case R.id.txt_map_playback: {
                    final String[] item = {"快", "中", "慢"};
                    new AlertDialog.Builder(BMapActivity.this)
                            .setTitle("回放速度")
                            .setItems(item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    txtPlayBack.setText(item[which]);
                                    timeSize = new Integer[]{50, 100, 150}[which];
                                }
                            })
                            .create()
                            .show();
                    break;
                }
                case R.id.txt_map_stop: {
                    final String[] item = {"不设置", "10分钟", "30分钟", "60分钟"};
                    new AlertDialog.Builder(BMapActivity.this)
                            .setTitle("停留标识")
                            .setItems(item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    txtStop.setText(item[which]);
                                    timeMin = new Integer[]{0, 10, 30, 60}[which];
                                }
                            })
                            .create()
                            .show();
                    break;
                }
                case R.id.txt_map_show: {
                    final String[] item = {"图标", "划线", "图标+画线"};
                    new AlertDialog.Builder(BMapActivity.this)
                            .setTitle("显示方式")
                            .setItems(item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    txtShow.setText(item[which]);
                                    showType = new Integer[]{1, 2, 3}[which];
                                }
                            })
                            .create()
                            .show();
                    break;
                }
            }
        }
    };

    /**
     * 根据数字返回方向
     *
     * @param s
     * @return
     */
    private String directionType(String s) {
        int d = Integer.parseInt(s);
        if ((d >= 337 && d <= 360) || (d >= 0 && d <= 23)) {
            return "北向";
        } else if (d >= 23 && d <= 68) {
            return "东北向";
        } else if (d >= 68 && d <= 113) {
            return "东向";
        } else if (d >= 113 && d <= 158) {
            return "东南向";
        } else if (d >= 158 && d <= 203) {
            return "南向";
        } else if (d >= 203 && d <= 248) {
            return "西南向";
        } else if (d >= 248 && d <= 293) {
            return "西向";
        } else //if (d >= 293 && d <= 338) {
            return "西北向";
    }

    /**
     * 根据数字判断定位类型
     *
     * @param s
     * @return
     */
    private String localType(String s) {
        if (s == null) return "GPS";
        switch (Integer.parseInt(s)) {
            case 0:
                return "LBS";
            case 1:
                return "GPS";
            case 2:
                return "LBS";
            case 3:
                return "WIFI";
            default:
                return "GPS";
        }
    }

    private int textureIndexs = 0;

    /**
     * 根据两个点，切割成数个小点
     *
     * @param local1
     * @param local2
     */
    private List<LatLng> addLatLng(LocalBean local1, LocalBean local2) {
        final Double a_x = Double.parseDouble(local1.getBaidu().split(",")[0]);
        final Double a_y = Double.parseDouble(local1.getBaidu().split(",")[1]);
        final Double b_x = Double.parseDouble(local2.getBaidu().split(",")[0]);
        final Double b_y = Double.parseDouble(local2.getBaidu().split(",")[1]);
        final Double distance = DistanceUtil.getDistance(new LatLng(a_x, a_y), new LatLng(b_x, b_y));
        final Double partX = Math.abs(a_x - b_x) / distance;
        final Double partY = Math.abs(a_y - b_y) / distance;
        final List<LatLng> list = new ArrayList<>();
        for (int i = 0; i < distance; i++) {
            if (i % 5 == 0) {
                Double x;
                if (a_x < b_x) x = a_x + partX * i;
                else if (a_x > b_x) x = a_x - partX * i;
                else x = a_x;
                Double y;
                if (a_y < b_y) y = a_y + partY * i;
                else if (a_y > b_y) y = a_y - partY * i;
                else y = a_y;
                list.add(new LatLng(x, y));
            }
        }
        return list;
    }

    /**
     * 把一段完整的坐标分段切割
     */
    private void setLatLng() {
        for (int i = 0; i < runLocal.size(); i++) {
            runPoints.add(getLatLng(runLocal.get(i)));
        }
    }

    /**
     * 根据字符转换颜色
     *
     * @param s
     * @return
     */
    private BitmapDescriptor getColor(String s) {
        BitmapDescriptor bd = null;
        switch (Integer.parseInt(s)) {
            case 0:
                bd = BitmapDescriptorFactory.fromAsset("icon_road_blue_arrow.png");//LBS
                break;
            case 1:
                bd = BitmapDescriptorFactory.fromAsset("icon_road_green_arrow.png");//GPS
                break;
            case 2:
                bd = BitmapDescriptorFactory.fromAsset("icon_road_blue_arrow.png");//LBS
                break;
            case 3:
                bd = BitmapDescriptorFactory.fromAsset("icon_road_red_arrow.png");//WIFI
                break;
            default:
                bd = BitmapDescriptorFactory.fromAsset("icon_road_green_arrow.png");//GPS
                break;
        }
        return bd;
    }

    int index = 0;//一组定位，由第一个点，绘制到index
    List<LatLng> options = new ArrayList<>();//已经绘制点的集合
    List<Integer> list_int = new ArrayList<>();
    Marker marker;

    /**
     * 开始/停止（划线）
     * 1.把获取的定位分割成若干个小定位点，见方法setLatLng()
     * 2.获取总坐标位置的长度，并记录第一个坐标点和颜色线段，设置进度条最大值为count就是坐标集合的数量
     * 3.开始-》显示中心坐标（由于安卓不停绘制图片会出现闪闪的，选择写死在中心点）,初始化计时器，计时器 的内容就是，一点一点往暂存集合加坐标去绘制线，并修改文字信息，修改进度条值
     * 4.暂停-》销毁计时器，隐藏中心坐标，绘制暂停时最后一个坐标为图标
     * 5.停止-》初始化所有相关数据，回到初始状态
     */
    private void startWire() {
        runBitmap.add(getColor("0"));
        runBitmap.add(getColor("1"));
        runBitmap.add(getColor("2"));
        runBitmap.add(getColor("3"));
        onClickMarker();
        setLatLng();
        marker = (Marker) mBaiduMap.addOverlay(new MarkerOptions().position(runPoints.get(0)).icon(BitmapDescriptorFactory.fromResource(icon)));
        final int count = runPoints.size();
        progressBar.setMax(count);
        options.add(runPoints.get(index));
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (STATE == 1) {
                    if (STOP_STATE==1){
                        mBaiduMap.clear();
                        marker = (Marker) mBaiduMap.addOverlay(new MarkerOptions().position(runPoints.get(0)).icon(BitmapDescriptorFactory.fromResource(icon)));
                        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(18f).build()));//定义层级
                        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(runPoints.get(0)));//定位显示到初始地点
                        STOP_STATE=0;
                        progressBar.setProgress(0);
                    }
                    //mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(runPoints.get(index)));//定位显示到初始地点
                    mileage = mileage+DistanceUtil.getDistance(runPoints.get(index), runPoints.get(index+1));
                    StringBuffer sb = new StringBuffer();
                    sb.append("时速：");
                    sb.append(runLocal.get(index).getSpd() + "KM ");//速度
                    sb.append(directionType(runLocal.get(index).getDeg()));//方向
                    sb.append(" 里程：" + new DecimalFormat("0.00").format(mileage / 1000) + "KM");//里程
                    sb.append(" " + localType(runLocal.get(index).getSource()));//定位类型
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("type", "1");
                    bundle.putString("speed", sb.toString());
                    bundle.putString("time", runLocal.get(index).getUpdatetime());
                    message.setData(bundle);
                    myHandler.sendMessage(message);
                    progressBar.setProgress(index);
                    cav(addLatLng(runLocal.get(index), runLocal.get(index + 1)), Integer.parseInt(runLocal.get(index).getSource()));
                    STATE = 2;
                    btnStart.setImageResource(R.mipmap.btn_pause);
                } else {
                    STATE = 1;
                    if (timer != null) {
                        timer.cancel();
                    }
                    btnStart.setImageResource(R.mipmap.btn_play);

                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart.setImageResource(R.mipmap.btn_play);
                if (timer != null) {
                    timer.cancel();
                }
                index = 0;
                cav_index = 0;
                list_int = new ArrayList<>();
                options = new ArrayList<LatLng>();
                options.add(runPoints.get(index));
                STATE = 1;
                STOP_STATE=1;
            }
        });
    }

    int cav_index = 0;
    List<LatLng> list_cav = new ArrayList<>();
    private Polyline polyline = null;
    private Polyline cav_polyline = null;

    /**
     * 初始化线条
     */
    private void initWire() {
        btnStart.setImageResource(R.mipmap.btn_play);
        if (timer != null) timer.cancel();
        index = 0;  //初始化大点index
        cav_index = 0;//初始化小点index
        options = new ArrayList<>();//初始化大坐标index
        progressBar.setProgress(0);//初始化进度条
        marker = null;//初始化icon
        STATE = 1;//初始化画线状态
        list_int = new ArrayList<>();//初始化线条点
        runBitmap = new ArrayList<>();//初始化颜色

    }


    private void cav(final List<LatLng> latLng, final int color_index) {
        cav_polyline = null;
        timer = new Timer();
        final List<Integer> list_cav_int = new ArrayList<>();
        list_cav = new ArrayList<>();
        if (latLng.size() != 0)
            list_cav.add(latLng.get(0));
        list_cav_int.add(color_index);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (cav_index == latLng.size() - 1 || latLng.size() == 0) {
                    timer.cancel();
                    cav_index = 0;
                    if (index != runPoints.size() - 1) {
                        index++;
                        list_cav = new ArrayList<>();
                        list_int.add(Integer.parseInt(runLocal.get(index).getSource()));
                        options.add(runPoints.get(index));
                        //mBaiduMap.clear();
                        //mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(runPoints.get(index)));//定位显示到初始地点
                        if (polyline != null) polyline.remove();
                        polyline = (Polyline) mBaiduMap.addOverlay(new PolylineOptions().width(10).points(options).dottedLine(true).customTextureList(runBitmap).textureIndex(list_int));//画线
                        if (index!=0)
                            mileage += DistanceUtil.getDistance(options.get(index-1), options.get(index));
                        StringBuffer sb = new StringBuffer();
                        sb.append("时速：");
                        sb.append(runLocal.get(index).getSpd() + "KM ");//速度
                        sb.append(directionType(runLocal.get(index).getDeg()));//方向
                        sb.append(" 里程：" + new DecimalFormat("0.00").format(mileage / 1000) + "KM");//里程
                        sb.append(" " + localType(runLocal.get(index).getSource()));//定位类型
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("type", "1");
                        bundle.putString("speed", sb.toString());
                        bundle.putString("time", runLocal.get(index).getUpdatetime());
                        message.setData(bundle);
                        myHandler.sendMessage(message);
                        progressBar.setProgress(index + 1);
                        try {
                            if (timeMin > 0) {
                                if (isDot) {
                                    int resource = getGPStoImage(localType(runLocal.get(index).getSource()));
                                    if (getDateMin(runLocal.get(index - 1).getUpdatetime(), runLocal.get(index).getUpdatetime()) > 1) {
                                        resource = R.mipmap.ic_pin_red;
                                    }
                                    MarkerOptions options = new MarkerOptions().position(getLatLng(runLocal.get(index))).icon(BitmapDescriptorFactory.fromResource(resource));
                                    Marker m = (Marker) mBaiduMap.addOverlay(options);
                                    Bundle bundle1 = new Bundle();
                                    bundle1.putInt("index", index);
                                    m.setExtraInfo(bundle1);
                                } else {
                                    if (getDateMin(runLocal.get(index - 1).getUpdatetime(), runLocal.get(index).getUpdatetime()) > 1) {
                                        int resource = R.mipmap.ic_pin_red;
                                        MarkerOptions options = new MarkerOptions().position(getLatLng(runLocal.get(index))).icon(BitmapDescriptorFactory.fromResource(resource));
                                        Marker m = (Marker) mBaiduMap.addOverlay(options);
                                        Bundle bundle1 = new Bundle();
                                        bundle1.putInt("index", index);
                                        m.setExtraInfo(bundle1);
                                    }
                                }
                            } else {
                                if (isDot) {
                                    int resource = getGPStoImage(localType(runLocal.get(index).getSource()));
                                    MarkerOptions options = new MarkerOptions().position(getLatLng(runLocal.get(index))).icon(BitmapDescriptorFactory.fromResource(resource));
                                    Marker m = (Marker) mBaiduMap.addOverlay(options);
                                    Bundle bundle1 = new Bundle();
                                    bundle1.putInt("index", index);
                                    m.setExtraInfo(bundle1);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (index != runLocal.size() - 1) {
                            cav(addLatLng(runLocal.get(index), runLocal.get(index + 1)), Integer.parseInt(runLocal.get(index + 1).getSource()));
                        } else {
                            Message message2 = new Message();
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("type", "2");
                            message2.setData(bundle2);
                            myHandler.sendMessage(message2);
                        }
                    }
                } else {
                    list_cav.add(latLng.get(cav_index));
                    if (cav_index<latLng.size()-1) {
                        Point pt = mBaiduMap.getMapStatus().targetScreen;
                        Point point = mBaiduMap.getProjection().toScreenLocation(latLng.get(cav_index+1));
                        if (point.x < 0 || point.x > pt.x * 2 || point.y < 0 || point.y > pt.y * 2) {
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putString("type", "4");
                            bundle.putDouble("lat",latLng.get(cav_index+1).latitude);
                            bundle.putDouble("lng",latLng.get(cav_index+1).longitude);
                            message.setData(bundle);
                            myHandler.sendMessage(message);
                            Log.d("TAG","即使祭祀时啊");
                        }
                    }
                    marker.setPosition(latLng.get(cav_index));
                    if (cav_polyline == null) {
                        cav_polyline = (Polyline) mBaiduMap.addOverlay(new PolylineOptions().width(10).points(list_cav).dottedLine(true).customTextureList(runBitmap).textureIndex(list_cav_int));//画线
                    } else {
                        cav_polyline.setPoints(list_cav);
                    }
                    list_cav.remove(1);
                    cav_index++;
                }
            }
        };
        timer.schedule(task, 0, timeSize);
    }

    String time = "";//停留时间
    List<Marker> markerList;


    /**
     * 开始/停止（打点）
     * 1.获取打点的经纬度，定位到这个点
     * 2.判断当前时间与上个时间的时间差是否大于多少分钟，大于则显示红标，否则显示定位类型标
     * 3.携带定位参数index，当前是第几个，放入Marker里面
     */
    private void startDot() {
        onClickMarker();
        markerList=new ArrayList<>();
        final int count = runLocal.size();
        progressBar.setMax(count);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (STATE == 1) {
                    timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            if (count == index) {
                                timer.cancel();
                                STATE = 1;
                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                bundle.putString("type", "2");
                                message.setData(bundle);
                                myHandler.sendMessage(message);
                                return;
                            }
                            if (STOP_STATE==1){
                                progressBar.setProgress(0);
                                markerList=new ArrayList<>();
                                mBaiduMap.clear();
                                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(18f).build()));//定义层级
                                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(getLatLng(runLocal.get(0))));//定位显示到初始地点
                                //marker = (Marker) mBaiduMap.addOverlay(new MarkerOptions().position(getLatLng(runLocal.get(0))).icon(BitmapDescriptorFactory.fromResource(icon)));
                                STOP_STATE=0;
                            }
                            LatLng latLng = getLatLng(runLocal.get(index));

                            if (index<runLocal.size()-1) {
                                Point pt = mBaiduMap.getMapStatus().targetScreen;
                                Point point = mBaiduMap.getProjection().toScreenLocation(getLatLng(runLocal.get(index + 1)));
                                if (point.x < 0 || point.x > pt.x * 2 || point.y < 0 || point.y > pt.y * 2) {
                                    Message message = new Message();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("type", "3");
                                    message.setData(bundle);
                                    myHandler.sendMessage(message);
                                }
                            }
                            int resource = getGPStoImage(localType(runLocal.get(index).getSource()));

                            //画Maeker并填充数据
                            Marker marker1 = (Marker) mBaiduMap.addOverlay(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(resource)));
                            Bundle bundle1 = new Bundle();
                            bundle1.putInt("index", index);
                            marker1.setExtraInfo(bundle1);
                            markerList.add(marker1);
                            markerList.get(index).setIcon(BitmapDescriptorFactory.fromResource(icon));

                            if (index>0){
                                resource = getGPStoImage(localType(runLocal.get(index-1).getSource()));
                                try {
                                    if (timeMin > 0 && index >1 && getDateMin(runLocal.get(index - 2).getUpdatetime(), runLocal.get(index-1).getUpdatetime()) > 1) {
                                        resource = R.mipmap.ic_pin_red;
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                markerList.get(index-1).setIcon(BitmapDescriptorFactory.fromResource(resource));
                            }
                            if (index!=0) {
                                //更新文字信息
                                mileage += DistanceUtil.getDistance(getLatLng(runLocal.get(index-1)), getLatLng(runLocal.get(index)));
                            }
                            StringBuffer sb = new StringBuffer();
                            sb.append("时速：");
                            sb.append(runLocal.get(index).getSpd() + "KM ");//速度
                            sb.append(directionType(runLocal.get(index).getDeg()));//方向
                            sb.append(" 里程：" + new DecimalFormat("0.00").format(mileage / 1000) + "KM");//里程
                            sb.append(" " + localType(runLocal.get(index).getSource()));//定位类型
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putString("type", "1");
                            bundle.putString("speed", sb.toString());
                            bundle.putString("time", runLocal.get(index).getUpdatetime());
                            message.setData(bundle);
                            myHandler.sendMessage(message);
                            index++;
                            progressBar.setProgress(index);//进度条信息
                        }
                    };
                    STATE = 2;
                    btnStart.setImageResource(R.mipmap.btn_pause);
                    timer.schedule(task, 0, timeSize * 10);
                } else {
                    timer.cancel();
                    STATE = 1;
                    btnStart.setImageResource(R.mipmap.btn_play);
                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart.setImageResource(R.mipmap.btn_play);
                if (timer != null) {
                    timer.cancel();
                }
                index = 0;
                STATE = 1;
               STOP_STATE=1;
            }
        });
    }

    /**
     * 初始化打点信息
     */
    private void initDot() {
        btnStart.setImageResource(R.mipmap.btn_play);
        if (timer != null) {
            timer.cancel();
        }
        index = 0;
        STATE = 1;
        progressBar.setProgress(0);
    }


    /**
     * 根据GPS定位类型返回对应图片
     *
     * @param s
     * @return
     */
    private int getGPStoImage(String s) {
        int resource = 0;
        if (s.equals("GPS")) {
            resource = R.mipmap.ic_pin_gps;
        } else if (s.equals("LBS")) {
            resource = R.mipmap.ic_pin_lbs;
        } else if (s.equals("WIFI")) {
            resource = R.mipmap.ic_pin_wifi;
        }
        return resource;
    }

    /**
     * 监听图标
     */
    private void onClickMarker() {
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                Log.d("TAG", "执行了监听图标方法");
                int indexMarker = 0;
                if (marker.getExtraInfo() != null) {
                    indexMarker = marker.getExtraInfo().getInt("index");
                }
                try {
                    if (indexMarker != 0)
                        time=getDateHSM(runLocal.get(indexMarker - 1).getUpdatetime(), runLocal.get(indexMarker).getUpdatetime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                GeoCoder geoCoder = GeoCoder.newInstance();
                final int finalIndexMarker = indexMarker;
                geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
                        View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.layout_map_details, null);
                        TextView txtDate = view.findViewById(R.id.txt_map_details_date);
                        TextView txtTime = view.findViewById(R.id.txt_map_details_time);
                        TextView txtAddress = view.findViewById(R.id.txt_map_details_address);
                        txtDate.setText(runLocal.get(finalIndexMarker).getUpdatetime() + " " + localType(runLocal.get(finalIndexMarker).getSource()));
                        if (timeMin>0){
                            txtTime.setText(time);
                        }else{
                            txtTime.setText("");
                        }

                        txtAddress.setText(arg0.getAddress());
                        //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
                        InfoWindow mInfoWindow = new InfoWindow(view, getLatLng(runLocal.get(finalIndexMarker)), -50);
                        //显示InfoWindow
                        mBaiduMap.showInfoWindow(mInfoWindow);
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBaiduMap.hideInfoWindow();
                            }
                        });
                    }

                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult arg0) {
                    }
                });
                //设置反地理编码位置坐标
                ReverseGeoCodeOption op = new ReverseGeoCodeOption();
                op.location(getLatLng(runLocal.get(indexMarker)));
                //发起反地理编码请求(经纬度->地址信息)
                geoCoder.reverseGeoCode(op);
                return false;
            }
        });
    }


    /**
     * 根据实体类转换坐标
     *
     * @param localBean
     * @return
     */
    private LatLng getLatLng(LocalBean localBean) {
        return new LatLng(Double.parseDouble(localBean.getBaidu().split(",")[0]), Double.parseDouble(localBean.getBaidu().split(",")[1]));
    }

    /**
     * 计算两个时间相差多少分钟
     *
     * @param endDate
     * @param endDate
     * @return
     */
    private long getDateMin(String startDate, String endDate) throws ParseException {
        Date date1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startDate);
        Date date2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endDate);
        long ns=1000;
        long nm = ns * 60;
        long nh = nm * 60;
        // 获得两个时间的毫秒时间差异
        long diff = date2.getTime() - date1.getTime();
        return diff/nm;
    }

    /**
     * 计算两个时间相差多少分钟
     *
     * @param endDate
     * @param endDate
     * @return
     */
    private String getDateHSM(String startDate, String endDate) throws ParseException {
        Date date1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startDate);
        Date date2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endDate);
        long ns=1000;
        long nm = ns * 60;
        long nh = nm * 60;
        long diff = date2.getTime() - date1.getTime();

        long mm = diff%nh / nm;

        long hh=diff/nh;

        long ss=diff%nh%nm/ns;
        if((mm+hh+ss)==0){
            return "";
        }else{
            StringBuffer sb=new StringBuffer("停留时间");
            if (hh!=0) sb.append(hh+"小时");
            if (mm!=0) sb.append(mm+"分钟");
            if (ss!=0) sb.append(ss+"秒");
            return sb.toString();
        }
    }


    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            if (bundle.getString("type").equals("1")) {
                txtSpeed.setText(bundle.getString("speed"));
                txtTime.setText("时间：" + bundle.getString("time"));
            } else if (bundle.getString("type").equals("2")) {
                btnStart.setImageResource(R.mipmap.btn_play);
                new AlertDialog.Builder(BMapActivity.this)
                        .setTitle("回放完毕")
                        .setMessage("开始时间：" + txtStartTime.getText().toString() + "\n" + "结束时间：" + txtEndTime.getText().toString() + "\n距离：" + new DecimalFormat("0.00").format(mileage / 1000) + "KM")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                STOP_STATE=1;
                                if (showType == 1) {
                                    initDot();
                                    startDot();
                                } else if (showType == 2) {
                                    isDot = false;
                                    initWire();
                                    startWire();
                                } else {
                                    initWire();
                                    isDot = true;
                                    startWire();
                                }
                                mileage=0.0;
                            }
                        })
                        .create()
                        .show();

            }else if (bundle.getString("type").equals("3")){
                mBaiduMap.hideInfoWindow();
                if (runLocal.size()!=0) {
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(getLatLng(runLocal.get(index + 1))));
                }
            }else if (bundle.getString("type").equals("4")){
                mBaiduMap.hideInfoWindow();
                LatLng latlng=new LatLng(bundle.getDouble("lat"),bundle.getDouble("lng"));
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latlng));
            }
            super.handleMessage(msg);
        }
    };


    //获取数据
    private List<LocalBean> getData() {
        String json = null;
        try {
            InputStream is = getAssets().open("local.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            json = new String(buf);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final List<LocalBean> list=new ArrayList<>();
         for(LocalBean l:GsonUtil.jsonToList(json, LocalBean.class)){
                    switch (LOCAL_TYPE){
                        case 0:{
                            list.add(l);
                            break;
                        }
                        case 1:{
                            if (l.getSource().equals("1")){
                                list.add(l);
                            }
                            break;
                        }
                        case 2:{
                            if (l.getSource().equals("3")){
                                list.add(l);
                            }
                            break;
                        }
                        case 3:{
                            if (l.getSource().equals("0") || l.getSource().equals("2")){
                                list.add(l);
                            }
                            break;
                        }
                        default:
                            if (l.getSource().equals("1")){
                                list.add(l);
                            }
                            break;
                    }
                }

           return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        timer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

}
