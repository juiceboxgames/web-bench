using System;
using System.Web;
using System.Threading;
using System.IO;
using System.IO.Compression;
using ServiceStack.Redis;
using ServiceStack.Text;
using Newtonsoft.Json;

class BenchmarkHandler : IHttpHandler
{

    public BenchmarkHandler()
    {
    }
    public void ProcessRequest(HttpContext context)
    {
	   HttpRequest Request = context.Request;
	   HttpResponse Response = context.Response;
	   // This handler is called whenever a file ending
	   // in .sample is requested. A file with that extension
	   // does not need to exist.


	   using (var redisClient = new RedisClient("10.174.178.235"))
       {
		   byte[] compressed = redisClient.Get("user_data_cs");
		   double k = 1;
		   for(double i = 0.0; i < 100.0; i+= 1.0){
				for(double j = 0.0; j < 100.0; j+= 1.0){
					k += Math.Sin(i) * Math.Sin(j);
				}
		   }
		   redisClient.Set("user_data_cs", compressed);
		   Response.Write("OK " + k);
       }

    }

	public static byte[] ZipStr(String str)
	{
		using (MemoryStream output = new MemoryStream())
		{
			using (DeflateStream gzip =
			  new DeflateStream(output, CompressionMode.Compress))
			{
				using (StreamWriter writer =
				  new StreamWriter(gzip, System.Text.Encoding.UTF8))
				{
					writer.Write(str);
				}
			}

			return output.ToArray();
		}
	}

	public static string UnZipStr(byte[] input)
	{
		using (MemoryStream inputStream = new MemoryStream(input))
		{
			using (DeflateStream gzip =
			  new DeflateStream(inputStream, CompressionMode.Decompress))
			{
				using (StreamReader reader =
				  new StreamReader(gzip, System.Text.Encoding.UTF8))
				{
					return reader.ReadToEnd();
				}
			}
		}
	}


       public bool IsReusable
       {
           // To enable pooling, return true here.
           // This keeps the handler in memory.
           get { return false; }
       }
}
