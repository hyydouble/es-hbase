package com.daqsoft;

import com.daqsoft.bean.DpsDataBean;
import com.daqsoft.bean.DpsFullDataBean;
import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppMain3 {

    private static final String tableName = "dps_data";
    private static final String familyName = "cf";
    private static final String status = "status";
    private static final String sourceId = "sourceId";
    private static final String involvedWord = "involvedWord";
    private static final String sourceType = "sourceType";
    private static final String indexType = "indexType";
    private static final String sourceName = "sourceName";
    private static final String sensibility= "sensibility";
    private static final String emotion = "emotion";
    private static final String isContent = "isContent";
    private static final String isTitle = "isTitle";
    private  static final String title = "title";
    private  static final String releasData = "releasData";
    private  static final String content = "content";
    private  static final String sourceAccounts = "sourceAccounts";
    private  static final String author = "author";
    private  static final String getDate = "getDate";
    private  static final String digest = "digest";
    private  static final String schemeVersion = "schemeVersion";
    private  static final String schemeId = "schemeId";


    private static Admin admin = null;
    private static Configuration configuration = null;
    private static Connection connection = null;

    /**
     * 初始化hbase连接的一些配置
     */
    static {
        try {
            configuration = HBaseConfiguration.create();
            connection = ConnectionFactory.createConnection(configuration);
            admin = connection.getAdmin();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据关键词查询es，返回文档对应的id
     * @param client
     * @param keyWord
     * @return
     */
    public static List<String> getByKeyWord(TransportClient client, String keyWord){
        ArrayList<String> strings = new ArrayList<String>();
        SearchResponse searchResponse = client.prepareSearch("dps_data")
                .setTypes("data")
//                .setQuery(QueryBuilders.matchAllQuery()).get();
                .setQuery(QueryBuilders.matchQuery("title", keyWord)).get();
//        .setQuery(QueryBuilders.matchPhraseQuery(keyWord))
        SearchHits hits = searchResponse.getHits();
        for(SearchHit hit: hits){
            String s = hit.getSourceAsString();
            System.out.println(s);

            String id = hit.getId();
            // 获取数据系统的id
//            System.out.println(id);
            System.out.println("---------------------------");
            strings.add(id);
        }
        return strings;
    }

    /**
     * 全量读取es索引数据
     */
    public static List<String> getAllIndexData(TransportClient client){
        ArrayList<String> strings = new ArrayList<String>();
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch("dps_data")
                .setTypes("data");

        searchRequestBuilder.addSort("_doc", SortOrder.ASC);
        searchRequestBuilder.setSize(100);
        searchRequestBuilder.setQuery(QueryBuilders.queryStringQuery("*:*"));
        searchRequestBuilder.setScroll(TimeValue.timeValueMinutes(1));

        SearchResponse searchResponse = searchRequestBuilder.get();
//        System.out.println("命中数量：" + searchResponse.getHits().getTotalHits());

        int count = 1;
        do{
//            System.out.println("第" + count + "次打印数据");
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit: hits.getHits()){
                String s = hit.getSourceAsString();
                System.out.println(hit.getSourceAsString());
                strings.add(s);
            }
            count++;

            searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                    .setScroll(TimeValue.timeValueMinutes(1)).execute().actionGet();
        } while (searchResponse.getHits().getHits().length != 0);

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(searchResponse.getScrollId());
        client.clearScroll(clearScrollRequest).actionGet();

        return strings;
    }

    /**
     * 获取es全部数据
     */
//    public static List<String> queryFullData(TransportClient client){
//        ArrayList<String> strings = new ArrayList<String>();
//        SearchResponse response = client.prepareSearch("dps_full_data")
//                .setTypes("full_data")
//                .setQuery(QueryBuilders.matchAllQuery()).get();
//        SearchHits hits = response.getHits();
//        for (SearchHit hit: hits){
//            String s = hit.getSourceAsString();
//            System.out.println(s);
//            JSONObject.parseObject(s, DpsFullDataBean.class);
//            strings.add(s);
//        }
//
//        return strings;
//    }

    /**
     * 将数据存入hbase
     * @param dpsDataBeans
     * @param table
     */
    private static void save2Hbase(List<DpsDataBean> dpsDataBeans, Table table){
        ArrayList<Put> puts = new ArrayList<Put>();
        for(DpsDataBean dpsDataBean: dpsDataBeans){
            System.out.println(dpsDataBean.getContent());
            System.out.println("---------------------------------------------------");
            Put put = new Put(Bytes.toBytes(dpsDataBean.getId()+ ""));
            if(dpsDataBean.getTitle() != null && dpsDataBean.getTitle() != ""){
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(status), Bytes.toBytes(dpsDataBean.getStatus()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(sourceId), Bytes.toBytes(dpsDataBean.getSourceId()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(involvedWord), Bytes.toBytes(dpsDataBean.getInvolvedWord()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(sourceType), Bytes.toBytes(dpsDataBean.getSourceType()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(indexType), Bytes.toBytes(dpsDataBean.getIndexType()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(sourceName), Bytes.toBytes(dpsDataBean.getSourceName()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(sensibility), Bytes.toBytes(dpsDataBean.getSensibility()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(emotion), Bytes.toBytes(dpsDataBean.getEmotion()));


                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(isContent), Bytes.toBytes(dpsDataBean.getIsContent()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(isTitle), Bytes.toBytes(dpsDataBean.getIsTitle()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(title), Bytes.toBytes(dpsDataBean.getTitle()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(releasData), Bytes.toBytes(dpsDataBean.getReleasDate()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(content), Bytes.toBytes(dpsDataBean.getContent()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(sourceAccounts), Bytes.toBytes(dpsDataBean.getSourceAccounts()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(author), Bytes.toBytes(dpsDataBean.getAuthor()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(getDate), Bytes.toBytes(dpsDataBean.getGetDate()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(digest), Bytes.toBytes(dpsDataBean.getDigest()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(schemeVersion), Bytes.toBytes(dpsDataBean.getSchemeVersion()));
                put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(schemeId), Bytes.toBytes(dpsDataBean.getSchemeId()));
                puts.add(put);
            }
        }
        try {
            table.put(puts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取表名
     * @return
     */
    private static Table getTable() throws IOException {
        HTableDescriptor hTableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
        HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(familyName);
        hTableDescriptor.addFamily(hColumnDescriptor);
        if(!admin.tableExists(TableName.valueOf(tableName))){
            admin.createTable(hTableDescriptor);
        }
        return connection.getTable(TableName.valueOf(tableName));

    }

    /**
     * 查询hbase表数据
     * @param tableName
     * @throws IOException
     */
//    public static void queryTable(String tableName) throws IOException {
//        Table table = connection.getTable(TableName.valueOf(tableName));
//        ResultScanner scanner = table.getScanner(new Scan());
//        for(Result result: scanner){
//            byte[] row = result.getRow();
//            System.out.println("row key is:" + new String(row));
//            List<Cell> cells = result.listCells();
//            for(Cell cell: cells){
//                byte[] familyArray = cell.getFamilyArray();
//                byte[] qualifierArray = cell.getQualifierArray();
//                byte[] valueArray = cell.getValueArray();
//                System.out.println("row value is:" + new String(familyArray) + new String(qualifierArray) + new String(valueArray));
//            }
//        }
//    }

    /**
     * 连接es老集群
     * @return
     * @throws Exception
     */
    public static TransportClient getOldEsClient() throws Exception {
        Settings settings = Settings.builder().put("cluster.name", "myes")
                .put("client.transport.sniff", true)
                .build();
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName("192.168.0.145"),9300));
        return client;
    }

    /**
     * 连接es新集群
     * @return
     */
    public static TransportClient getNewEsClient() throws Exception {
        Settings settings = Settings.builder().put("cluster.name", "my-application")
                .put("client.transport.sniff", true)
                .build();
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName("daqsoft-bdsp-01"),9300));
        return client;
    }

//    private static void save2es(List<Doc> docInfo, TransportClient client){
//        BulkRequestBuilder bulk = client.prepareBulk();
//        for(Doc doc: docInfo){
//            IndexRequestBuilder indexRequestBuilder = client.prepareIndex("tftj", "doc");
//            Gson gson = new Gson();
//            String jsonStr = gson.toJson(doc);
//            indexRequestBuilder.setSource(jsonStr, XContentType.JSON);
//            bulk.add(indexRequestBuilder);
//        }
//        // 触发数据真正保存早es中去
//        BulkResponse bulkItemResponses = bulk.get();
//        client.close();
//    }

    /**
     *向es中添加数据创建索引
     * @param index
     * @param type
     * @param dpsFullDataBean
     */
    public static String save2es(String index, String type, DpsFullDataBean dpsFullDataBean) throws Exception {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("id", dpsFullDataBean.getId());
        hashMap.put("title", dpsFullDataBean.getTitle());
        hashMap.put("digest", dpsFullDataBean.getDigest());
        hashMap.put("sourceName", dpsFullDataBean.getSourceName());

        IndexResponse response = getNewEsClient().prepareIndex(index, type, String.valueOf(dpsFullDataBean.getId())).setSource(hashMap).execute().actionGet();
        return response.getId();

    }

    /**
     * 解析json数据
     */
    public static List<DpsFullDataBean> parseJson() throws Exception {
        TransportClient client = getOldEsClient();
        List<String> jsonArray = getAllIndexData(client);
        System.out.println(jsonArray.size());
        List<DpsFullDataBean> dpsFullDataBeanList = new ArrayList<DpsFullDataBean>();
        for (String json: jsonArray){
            Gson gson = new Gson();
            DpsFullDataBean dpsFullDataBean = gson.fromJson(json, DpsFullDataBean.class);
//            DpsFullDataBean dpsFullDataBean = JSONObject.parseObject(json, DpsFullDataBean.class);
            dpsFullDataBeanList.add(dpsFullDataBean);
        }
        return dpsFullDataBeanList;
    }

    /**
     * 根据es返回的id查询hbase中的数据
     */
    public static void queryHbaseByEsId() throws Exception {
        TransportClient client = getNewEsClient();
        Table table = getTable();
        List<String> keyWords = getByKeyWord(client, "haha");
        System.out.println(keyWords);
        for(String keyWord: keyWords){
            Get get = new Get(Bytes.toBytes(keyWord));
            Result result = table.get(get);
            KeyValue[] values = result.raw();
            for(KeyValue value: values){
//                byte[] qualifierArray = value.getQualifierArray();
                System.out.println(new String(value.getValue()));
            }
            System.out.println("-------------------------------");
        }
    }

    /**
     * 程序入口
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Table table = getTable();
//        for (Doc doc: docInfo){
//            System.out.println(doc);
//            String id = save2es("zsp", "doc", doc);
//            System.out.println(id);
//        }

//        save2Hbase(docInfo, table);
//        queryTable("doc");

        List<DpsFullDataBean> dpsFullDataBeanList = parseJson();
//        for (DpsFullDataBean dpsFullDataBean: dpsFullDataBeanList){
//            System.out.println(dpsFullDataBean);
//            save2es("full_data_2", "full_data", dpsFullDataBean);
//        }

//        save2Hbase(dpsFullDataBeanList, table);

//        System.out.println("---------------------------------------");

//        for (DpsFullDataBean dpsFullDataBean: dpsFullDataBeanList){
//            save2es("full_data_2", "full_data", dpsFullDataBean);
//        }

        queryHbaseByEsId();
    }

}
