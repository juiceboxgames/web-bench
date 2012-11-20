# config.ru
# run with `thin start -p 9292`

require "redis"
require "json"
require "zlib"

class App
  def call(env)
  	redis = Redis.new(:host => "10.174.178.235", :port => 6379)
  	val = redis.get "user_data_ruby"
  	uncompressed = Zlib::Inflate.inflate(val)
  	obj = JSON.parse(uncompressed)
  	for i in 0..100
         for j in 0..100
            k = Math.sin(i) * Math.sin(j)
         end
      end
  	jsonEncoded = JSON.generate(obj)
  	compressed = Zlib::Deflate.deflate(jsonEncoded)
  	redis.set "user_data_ruby", val
    [200, {'Content-Type' => 'text/plain'}, ["OK"]]
  end
end

run App.new