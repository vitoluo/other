jenkins使用遇见的问题
---
1. 有子模块（submodule）的仓库不能使用浅克隆（sallow），不然会导致拉取代码失败
2. 有配置gitParameter的仓库每次都需要进行拉取，不然会导致获取不到分支