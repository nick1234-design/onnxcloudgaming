package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

object StreamingSettings {
    private const val PREFS = "onnx_streaming_settings"
    private const val KEY_RESOLUTION = "target_resolution"
    private const val KEY_H264 = "prefer_h264"

    fun show(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val resolution = prefs.getString(KEY_RESOLUTION, "auto") ?: "auto"
        val resolutionLabel = when (resolution) {
            "720p" -> "720p"
            "1080p" -> "1080p"
            else -> "Auto"
        }
        val codecLabel = if (prefs.getBoolean(KEY_H264, true)) {
            "H.264 preferred"
        } else {
            "Automatic"
        }

        AlertDialog.Builder(activity)
            .setTitle("Streaming settings")
            .setItems(arrayOf(
                "Target resolution: $resolutionLabel",
                "Video codec: $codecLabel"
            )) { _, which ->
                if (which == 0) showResolutionPicker(activity)
                else showCodecPicker(activity)
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun showResolutionPicker(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val values = arrayOf("auto", "720p", "1080p")
        val labels = arrayOf("Auto (recommended)", "720p", "1080p")
        val current = values.indexOf(prefs.getString(KEY_RESOLUTION, "auto")).coerceAtLeast(0)

        AlertDialog.Builder(activity)
            .setTitle("Target resolution")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                prefs.edit().putString(KEY_RESOLUTION, values[which]).apply()
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    "Resolution preference saved. It applies when you launch Cloud Gaming.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showCodecPicker(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val labels = arrayOf("H.264 preferred (recommended)", "Automatic codec order")
        val current = if (prefs.getBoolean(KEY_H264, true)) 0 else 1

        AlertDialog.Builder(activity)
            .setTitle("Video codec preference")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                prefs.edit().putBoolean(KEY_H264, which == 0).apply()
                dialog.dismiss()
                Toast.makeText(
                    activity,
                    "Codec preference saved. Automatic fallback is retained.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    fun buildInjectionScript(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val resolution = prefs.getString(KEY_RESOLUTION, "auto") ?: "auto"
        val h264 = prefs.getBoolean(KEY_H264, true)
        val dimensions = when (resolution) {
            "720p" -> Pair(1280, 720)
            "1080p" -> Pair(1920, 1080)
            else -> null
        }
        val width = dimensions?.first ?: 0
        val height = dimensions?.second ?: 0

        return """
            (function() {
              if (window.__onnxCloudPrefsInstalled) return;
              var proto = window.RTCPeerConnection && window.RTCPeerConnection.prototype;
              if (!proto || typeof proto.setLocalDescription !== "function") {
                console.info("ONNXCloudGaming: WebRTC preference hook unavailable on this page.");
                return;
              }
              var original = proto.setLocalDescription;
              var preferH264 = $h264;
              var targetWidth = $width;
              var targetHeight = $height;

              proto.setLocalDescription = function(description) {
                if (!description || typeof description.sdp !== "string") {
                  return original.apply(this, arguments);
                }

                try {
                  var lines = description.sdp.split(/\r?\n/);
                  var videoIndex = lines.findIndex(function(line) {
                    return line.indexOf("m=video ") === 0;
                  });

                  if (videoIndex >= 0) {
                    var parts = lines[videoIndex].trim().split(/\s+/);
                    var payloads = parts.slice(3);
                    var codecByPayload = {};
                    var profileByPayload = {};

                    lines.forEach(function(line) {
                      var map = line.match(/^a=rtpmap:(\d+)\s+([^/]+)/i);
                      if (map) codecByPayload[map[1]] = map[2].toUpperCase();
                      var fmtp = line.match(/^a=fmtp:(\d+)\s+.*profile-level-id=([0-9a-f]{6})/i);
                      if (fmtp) profileByPayload[fmtp[1]] = fmtp[2].toLowerCase();
                    });

                    if (preferH264) {
                      var h264Payloads = payloads.filter(function(id) {
                        return codecByPayload[id] === "H264";
                      });
                      var otherPayloads = payloads.filter(function(id) {
                        return codecByPayload[id] !== "H264";
                      });
                      h264Payloads.sort(function(a, b) {
                        function rank(id) {
                          var profile = profileByPayload[id] || "";
                          if (profile.indexOf("4d") === 0) return 0;
                          if (profile.indexOf("42e") === 0) return 1;
                          if (profile.indexOf("420") === 0) return 2;
                          return 3;
                        }
                        return rank(a) - rank(b);
                      });
                      if (h264Payloads.length) {
                        lines[videoIndex] = parts.slice(0, 3)
                          .concat(h264Payloads, otherPayloads).join(" ");
                      }
                    }

                    if (targetWidth > 0 && targetHeight > 0) {
                      var currentVideoPayloads = lines[videoIndex].trim().split(/\s+/).slice(3);
                      var imageAttributes = currentVideoPayloads.map(function(id) {
                        return "a=imageattr:" + id + " recv [x=" +
                          targetWidth + ",y=" + targetHeight + "]";
                      });
                      var alreadyHasImageAttr = lines.some(function(line) {
                        return line.indexOf("a=imageattr:") === 0;
                      });
                      if (!alreadyHasImageAttr) {
                        lines.splice.apply(lines, [videoIndex + 1, 0].concat(imageAttributes));
                      }
                    }

                    var patchedSdp = lines.join("\r\n");
                    var patchedDescription = window.RTCSessionDescription
                      ? new RTCSessionDescription({ type: description.type, sdp: patchedSdp })
                      : { type: description.type, sdp: patchedSdp };
                    var args = Array.prototype.slice.call(arguments, 1);
                    window.__onnxCloudPrefsApplied = true;
                    return original.apply(this, [patchedDescription].concat(args));
                  }
                } catch (error) {
                  console.warn("ONNXCloudGaming: Could not apply stream preferences.", error);
                }
                return original.apply(this, arguments);
              };

              window.__onnxCloudPrefsInstalled = true;
              console.info("ONNXCloudGaming: stream preferences hook installed; resolution=" +
                "$resolution, h264Preferred=" + preferH264);
            })();
        """.trimIndent()
    }
}
