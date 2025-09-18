FROM public.ecr.aws/docker/library/nginx:stable
COPY . /usr/share/nginx/html
