FROM nextcloud:23.0.3

RUN sed -i 's#http://deb.debian.org#https://mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && sed -i 's#http://security.debian.org#https://mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y libmagickcore-6.q16-6-extra
