// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

caja.configure({
  cajaServer: 'http://localhost:8000/caja',
  debug: true
}, function(frameGroup) {
  frameGroup.makeES5Frame(
      createDiv(),
      {
        rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
      },
      function(frame) {
        frame.url('es53-test-domita-events-guest.html')
             .run(createExtraImportsForTesting(frameGroup, frame),
                 function(result) {
                   readyToTest();
                   jsunitRun();
                   asyncRequirements.evaluate();
                 });
      });
});
