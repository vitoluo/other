mongo使用副本集的集群模式
---
1. 生成key
```shell
cd data/mongo
openssl rand -base64 756 > mongod.key
## 400权限是要保证安全性，否则mongod启动会报错
chmod 400 mongod.key
## mongo容器使用uid 999的用户
chown 999 mongod.key
```
2. 启动容器
3. 配置集群
```shell
# 选择第一个容器mongo1，进入mongo 容器
docker exec -it mongo1 mongosh
use admin
db.auth('${USERNAME'}, '${PASSWORD}')
# ip 需要使用宿主机对外提供服务的ip，不然外部无法使用
rs.initiate(
{
  _id: 'mongoRS',
  members: [
    {_id: 0, host: '${ip}:27017', priority: 5},
    {_id: 1, host: '${ip}:27018'},
    {_id: 2, host: '${ip}:27019'}
  ]
}
)
```
