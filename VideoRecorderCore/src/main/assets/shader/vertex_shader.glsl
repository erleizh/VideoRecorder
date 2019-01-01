uniform mat4 u_texMatrix;
uniform mat4 u_projectionViewMatrix;
attribute vec4 a_position;
attribute vec4 a_texCoord0;
varying vec2 v_texCoords;
void main()
{
   v_texCoords = (a_texCoord0 * u_texMatrix).xy;
   gl_Position =  u_projectionViewMatrix * a_position;
}
