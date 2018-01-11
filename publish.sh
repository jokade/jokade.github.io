#! /bin/sh
bundle exec jekyll build --destination ../blog-build

cd ../blog-build
git checkout -- Gemfile*

git status

echo
echo "continue? all untracked files will be added (y/n)"
read res
if [ "$res" == y ]
then
  git commit -a
  git push
fi
