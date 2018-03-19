precision mediump float;
uniform sampler2D sTexture0;//纹理内容数据
uniform sampler2D sTexture1;//纹理内容数据
uniform sampler2D sTexture2;//纹理内容数据
uniform sampler2D sTexture3;//纹理内容数据
uniform sampler2D sTexture4;//纹理内容数据

varying vec2 vTextureCoord; //接收从顶点着色器过来的参数
varying float vSerial;

void main()
{
   vec2 texCoords = vTextureCoord;

   //给此片元从纹理中采样出颜色值
   float mod_val = mod(vSerial,25.0);

   if(mod_val <= 5.0)
       gl_FragColor = texture2D(sTexture0, texCoords);
   else if(mod_val <= 10.0)
       gl_FragColor = texture2D(sTexture1, texCoords);
   else if(mod_val <= 15.0)
       gl_FragColor = texture2D(sTexture2, texCoords);
   else if(mod_val <= 20.0)
       gl_FragColor = texture2D(sTexture3, texCoords);
   else
       gl_FragColor = texture2D(sTexture4, texCoords);


}