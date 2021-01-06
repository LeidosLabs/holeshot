#version 400 core

in VertexData {
   vec2 uniformCoord;
} fs_in;


const int INTERPOLATION_NEAREST = 0;
const int INTERPOLATION_LINEAR = 1;
const int INTERPOLATION_BICUBIC = 2;
const int INTERPOLATION_CATMULL = 3;

uniform sampler2D rawImage;
out vec4 FragColor;
uniform int interpolation;
uniform ivec2 fbDim;

float Triangular( float f )
{
	f = f / 2.0;
	if( f < 0.0 )
	{
		return ( f + 1.0 );
	}
	else
	{
		return ( 1.0 - f );
	}
	return 0.0;
}

float CatMullRom( float x )
{
    float B = 0.0;
    float C = 0.5;
    float f = x;
    if( f < 0.0 )
    {
        f = -f;
    }
    if( f < 1.0 )
    {
        return ( ( 12.0 - 9.0 * B - 6.0 * C ) * ( f * f * f ) +
            ( -18.0f + 12.0f * B + 6.0f * C ) * ( f * f ) +
            ( 6.0f - 2.0f * B ) ) / 6.0f;
    }
    else if( f >= 1.0 && f < 2.0 )
    {
        return ( ( -B - 6 * C ) * ( f * f * f )
            + ( 6 * B + 30 * C ) * ( f *f ) +
            ( - ( 12 * B ) - 48 * C  ) * f +
            8 * B + 24 * C)/ 6.0;
    }
    else
    {
        return 0.0;
    }
}


vec4 BiCubic(vec2 TexCoord, bool useCatMull )
{
	float texelSizeX = 1.0 / float(fbDim.x); //size of one texel 
	float texelSizeY = 1.0 / float(fbDim.y); //size of one texel 
    vec4 nSum = vec4( 0.0, 0.0, 0.0, 0.0 );
    vec4 nDenom = vec4( 0.0, 0.0, 0.0, 0.0 );
    float a = fract( TexCoord.x * float(fbDim.x) ); // get the decimal part
    float b = fract( TexCoord.y * float(fbDim.y) ); // get the decimal part

	int nX = int(TexCoord.x * float(fbDim.x));
	int nY = int(TexCoord.y * float(fbDim.y));
	vec2 TexCoord1 = vec2( float(nX) / float(fbDim.x) + 0.5 / float(fbDim.x),
					       float(nY) / float(fbDim.y) + 0.5 / float(fbDim.y) );

    for( int m = -1; m <=2; m++ )
    {
        for( int n =-1; n<= 2; n++)
        {
			vec4 vecData = texture2D(rawImage, TexCoord1 + vec2(texelSizeX * float( m ), texelSizeY * float( n )));
			float f;
			float f1;
			if (useCatMull) {
			   f  = CatMullRom( float( m ) - a );
			   f1 = CatMullRom( -( float( n ) - b ) );
			} else {
			   f  = Triangular( float( m ) - a );
			   f1 = Triangular( -( float( n ) - b ) );
			}
			vec4 vecCooef1 = vec4( f,f,f,f );
			vec4 vecCoeef2 = vec4( f1, f1, f1, f1 );
            nSum = nSum + ( vecData * vecCoeef2 * vecCooef1  );
            nDenom = nDenom + (( vecCoeef2 * vecCooef1 ));
        }
    }
    return nSum / nDenom;
}


void main()
{
   vec2 texturePosition = (fs_in.uniformCoord.xy + 1.0) / 2.0;

     //FragColor = texture(rawImage, texturePosition);

   switch (interpolation) {
      case INTERPOLATION_NEAREST:
      case INTERPOLATION_LINEAR:
        // These are taken care of in hardware and are switched in the code
        FragColor = texture2D(rawImage, texturePosition);
         break;
      case INTERPOLATION_BICUBIC:
	     FragColor = BiCubic(texturePosition, false );
	     break;
      case INTERPOLATION_CATMULL:
	     FragColor = BiCubic(texturePosition, true );
	     break;
   }
   
}




