import os
import time
import requests


if __name__ == "__main__":
    sourceUrl = "https://github.com/fmz200/wool_scripts/raw/main/QuantumultX/rewrite/chongxie.txt"
    targetFile = "ad.snippet"
    needAds = {
        "12306",
        "Alibaba",
        "spotify",
        "百度",
        "百度贴吧",
        "哔哩哔哩",
        "钉钉",
        "饿了么",
        "飞猪 + 阿里巴巴",
        "高德地图",
        "谷歌",
        "建行生活",
        "京东",
        "夸克",
        "联想",
        "美团",
        "美团外卖",
        "米家",
        "拼多多",
        "起点读书",
        "汽车之家",
        "什么值得买",
        "淘宝",
        "腾讯广告",
        "天府通",
        "网上国网",
        "网易云音乐",
        "微信",
        "闲鱼",
        "小米",
        "小米商城",
        "小米有品",
        "小米运动",
        "一淘",
        "掌上生活",
        "招商银行",
        "知乎",
        "中国电信",
        "中国银行 缤纷生活",
        "中国银行",
        "字节跳动",
    }

    if os.path.exists(targetFile):
        os.remove(targetFile)
    with open(targetFile, "w", encoding="utf-8") as f:
        f.write("#!name=开屏广告拦截")
        f.write("\n")
        f.write("#!desc=规则来自fmz200，根据个人需要精简")
        f.write("\n")
        f.write("#!author=vito")
        f.write("\n")
        f.write("#!homepage=https://github.com/vitoluo/other")
        f.write("\n")
        f.write(
            "#!raw-url=https://github.com/vitoluo/other/raw/master/QuantumultX/ad.snippet"
        )
        f.write("\n")
        f.write("#!remark=原始规则地址：" + sourceUrl)
        f.write("\n")
        f.write("\n")

        resp = requests.get(sourceUrl, stream=True)
        matchAd = False
        if resp.status_code == 200:
            for l in resp.iter_lines():
                line = l.decode("utf-8")
                if line.startswith("hostname ="):
                    f.write(line)
                    f.write("\n")
                    f.write("\n")
                elif line.startswith("# >"):
                    name = line.replace("# > ", "")
                    if name in needAds:
                        matchAd = True
                        f.write(line)
                        f.write("\n")
                    else:
                        matchAd = False
                elif matchAd:
                    f.write(line)
                    f.write("\n")
        else:
            print("文件更新失败")
