# 娴锋磱鐢熺墿鐭ヨ瘑搴?- 鍚姩璇存槑

## 1. 鐜瑕佹眰

| 缁勪欢 | 鐗堟湰瑕佹眰 |
|------|---------|
| JDK | 1.8 |
| Maven | 3.6+ |
| Node.js | 16+ |
| MySQL | 8.0 |
| Redis | 5.x |
| ElasticSearch | 7.17.x |
| RocketMQ | 5.1.x |

## 2. 鏂瑰紡涓€锛氭湰鍦板紑鍙戝惎鍔?

### 2.1 鍚姩涓棿浠?

鍏堢‘淇濇湰鍦板凡瀹夎骞跺惎鍔?MySQL銆丷edis銆丒lasticSearch銆丷ocketMQ銆?

### 2.2 鍒濆鍖栨暟鎹簱

```bash
# 鐧诲綍 MySQL锛屾墽琛屽垵濮嬪寲鑴氭湰
mysql -u root -p < ocean-server/src/main/resources/sql/init.sql
```

鑴氭湰浼氳嚜鍔ㄥ垱寤?`ocean_knowledge` 鏁版嵁搴撱€? 寮犺〃锛屽苟鎻掑叆绠＄悊鍛樺垵濮嬭处鍙枫€?

### 2.3 閰嶇疆鍚庣

缂栬緫 `ocean-server/src/main/resources/application-dev.yml`锛屾寜瀹為檯鐜淇敼锛?

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ocean_knowledge?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 浣犵殑MySQL瀵嗙爜    # 榛樿 root
  redis:
    host: localhost
    port: 6379
  elasticsearch:
    uris: http://localhost:9200
  rocketmq:
    name-server: localhost:9876
```

**AI 鍔熻兘閰嶇疆**锛堝彲閫夛紝涓嶉厤缃椂 AI 鎺ュ彛浼氭姤閿欎絾涓嶅奖鍝嶅叾浠栧姛鑳斤級锛?

```yaml
ai:
  deepseek:
    api-key: 浣犵殑DeepSeek API Key
```

涔熷彲閫氳繃鐜鍙橀噺璁剧疆锛歚set DEEPSEEK_API_KEY=sk-xxx`

### 2.4 鍚姩鍚庣

```bash
cd ocean-server
mvn spring-boot:run
```

鍚庣鍚姩鎴愬姛鍚庤闂細`http://localhost:8080`

Swagger API 鏂囨。锛歚http://localhost:8080/doc.html`

### 2.5 鍚姩鍓嶇

```bash
cd ocean-web
npm install
npm run dev
```

鍓嶇寮€鍙戞湇鍔″櫒鍚姩鍚庤闂細`http://localhost:3000`

## 3. 鏂瑰紡浜岋細Docker Compose 涓€閿儴缃?

### 3.1 鏋勫缓鍓嶇

```bash
cd ocean-web
npm install
npm run build
cd ..
```

### 3.2 鏋勫缓鍚庣 JAR

```bash
cd ocean-server
mvn clean package -DskipTests
cd ..
```

### 3.3 閰嶇疆 AI Key锛堝彲閫夛級

```bash
# Windows PowerShell
$env:DEEPSEEK_API_KEY="sk-xxx"

# Linux/Mac
export DEEPSEEK_API_KEY=sk-xxx
```

### 3.4 涓€閿惎鍔?

```bash
docker-compose up -d
```

鍚姩瀹屾垚鍚庤闂細`http://localhost`

### 3.5 鏌ョ湅鏃ュ織

```bash
# 鏌ョ湅鎵€鏈夋湇鍔＄姸鎬?
docker-compose ps

# 鏌ョ湅鍚庣鏃ュ織
docker-compose logs -f ocean-server

# 鍋滄鎵€鏈夋湇鍔?
docker-compose down
```

## 4. 榛樿璐﹀彿

| 瑙掕壊 | 鐧诲綍鍚?| 瀵嗙爜 |
|------|--------|------|
| 绠＄悊鍛?| admin | admin123 |

> 瀵嗙爜浣跨敤 MD5 + 鐩愬€?`ocean_knowledge_salt_2026` 鍔犲瘑瀛樺偍銆?

## 5. 鏈嶅姟绔彛涓€瑙?

| 鏈嶅姟 | 鏈湴寮€鍙戠鍙?| Docker 绔彛 |
|------|------------|------------|
| 鍓嶇 (Vue) | 3000 | 80 (Nginx) |
| 鍚庣 (Spring Boot) | 8080 | 8080 |
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |
| ElasticSearch | 9200 | 9200 |
| RocketMQ NameServer | 9876 | 9876 |
| RocketMQ Broker | 10911 | 10911 |

## 6. ElasticSearch 绱㈠紩鍒濆鍖?

棣栨浣跨敤鍏ㄦ枃妫€绱㈠姛鑳藉墠锛岄渶鎵嬪姩鍒涘缓 ES 绱㈠紩锛?

```bash
curl -X PUT "http://localhost:9200/doc_index" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "name": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "content": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "ebookId": { "type": "long" }
    }
  }
}'
```

> 闇€瑕?ES 瀹夎 IK 鍒嗚瘝鍣ㄦ彃浠躲€傚鏈畨瑁咃紝鍙皢 analyzer 鏀逛负 `standard`锛?
> ```bash
> curl -X PUT "http://localhost:9200/doc_index" -H "Content-Type: application/json" -d '{
>   "mappings": {
>     "properties": {
>       "name": { "type": "text" },
>       "content": { "type": "text" },
>       "ebookId": { "type": "long" }
>     }
>   }
> }'
> ```

## 7. 甯歌闂

### Q: 鍚庣鍚姩鎶?`Communications link failure`
**A**: MySQL 鏈惎鍔ㄦ垨杩炴帴閰嶇疆閿欒锛屾鏌?`application-dev.yml` 涓殑鏁版嵁搴撳湴鍧€鍜屽瘑鐮併€?

### Q: 鍚庣鍚姩鎶?`Unable to connect to Redis`
**A**: Redis 鏈惎鍔紝鎵ц `redis-server` 鍚姩 Redis銆?

### Q: AI 闂瓟鍔熻兘鎶ラ敊
**A**: 闇€瑕侀厤缃?DeepSeek API Key銆傚湪 `application-dev.yml` 涓缃?`ai.deepseek.api-key`锛屾垨閫氳繃鐜鍙橀噺 `DEEPSEEK_API_KEY` 浼犲叆銆備笉閰嶇疆鏃跺叾浠栧姛鑳芥甯镐娇鐢ㄣ€?

### Q: 鎼滅储鍔熻兘鎶ラ敊
**A**: 闇€瑕佸惎鍔?ElasticSearch 骞跺垱寤?`doc_index` 绱㈠紩锛堣绗?6 鑺傦級銆?

### Q: Docker 鍚姩鍚?MySQL 鍒濆鍖栧け璐?
**A**: 濡傛灉鏁版嵁鍗峰凡瀛樺湪鏃ф暟鎹紝MySQL 涓嶄細閲嶆柊鎵ц init.sql銆傛墽琛?`docker-compose down -v` 鍒犻櫎鏁版嵁鍗峰悗閲嶆柊鍚姩銆?

### Q: 鍓嶇椤甸潰绌虹櫧
**A**: 纭繚鍚庣宸插惎鍔紝涓斿墠绔唬鐞嗛厤缃纭€傚紑鍙戞ā寮忎笅鍓嶇閫氳繃 `vue.config.js` 涓殑 proxy 灏?`/api` 璇锋眰浠ｇ悊鍒?`http://localhost:8080`銆?


