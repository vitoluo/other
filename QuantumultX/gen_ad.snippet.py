import os


def read_from_line(file_path, start_line):
    with open(file_path, "r", encoding="utf-8") as file:
        for _ in range(start_line - 1):
            next(file)
        for line in file:
            yield line.strip()


if __name__ == "__main__":
    sourceFile = "D:\download\chongxie.txt"
    targetFile = "D:\download\\ad.snippet"
    needAds = [
        "12306",
        "百度贴吧",
        "哔哩哔哩",
        "钉钉",
        "饿了么",
        "飞猪 + 阿里巴巴",
        "高德地图",
        "建行生活",
        "京东",
        "夸克",
        "联想",
        "美团",
        "美团外卖",
        "米家",
        "拼多多",
        "汽车之家",
        "起点读书",
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
        "米家",
        "一淘",
        "招商银行",
        "掌上生活",
        "中国银行",
        "中国银行 缤纷生活",
        "中国电信",
    ]

    adNameDict = {}
    hostnameSet = set()

    with open(sourceFile, "r", encoding="utf-8") as f:
        for num, line in enumerate(f, 1):
            if line.startswith("# > "):
                name = line.replace("# > ", "").replace("\n", "")
                adNameDict[name] = num

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
        f.write(
            "#!remark=原始规则地址：https://github.com/fmz200/wool_scripts/raw/main/QuantumultX/rewrite/chongxie.txt"
        )
        f.write("\n")
        f.write("\n")

        for adName in needAds:
            lineNum = adNameDict[adName]
            if lineNum is not None:
                count = 0
                for line in read_from_line(sourceFile, lineNum):
                    if count > 0 and line == "":
                        break
                    if count > 0 and line.startswith("# >"):
                        break
                    f.write(line)
                    f.write("\n")
                    if line.startswith("# hostname"):
                        for hostname in line.replace("# hostname =", "").split(","):
                            hostnameSet.add(hostname.strip())
                    count += 1
                f.write("\n")
        f.write("hostname = ")
        hostnames = ",".join(hostnameSet)
        f.write(hostnames)
