/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx.graphics.g3d.model.data;

import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.utils.Array;
import com.erlei.gdx.utils.ArrayMap;
import com.erlei.gdx.utils.IntArray;

public class ModelNodePart {
	public String materialId;
	public String meshPartId;
	public ArrayMap<String, Matrix4> bones;
	public int uvMapping[][];
}
