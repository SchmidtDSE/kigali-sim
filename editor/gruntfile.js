module.exports = function (grunt) {
  grunt.loadNpmTasks("grunt-contrib-qunit");
  grunt.loadNpmTasks("grunt-contrib-connect");

  grunt.initConfig({
    pkg: grunt.file.readJSON("package.json"),
    connect: {
      server: {
        options: {
          port: 8000,
        },
      },
    },
    qunit: {
      withSandbox: {
        options: {
          urls: ["http://localhost:8000/test/test.html"],
          timeout: 60000,
          puppeteer: {
            args: [],
          },
        },
      },
      withoutSandbox: {
        options: {
          urls: ["http://localhost:8000/test/test.html"],
          timeout: 60000,
          puppeteer: {
            args: ["--no-sandbox", "--disable-setuid-sandbox"],
          },
        },
      },
    },
  });

  grunt.registerTask("qunit-with-fallback", function() {
    var done = this.async();
    
    grunt.log.writeln("Attempting to run Chrome with sandbox enabled...");
    
    grunt.util.spawn({
      grunt: true,
      args: ["qunit:withSandbox"],
    }, function(error, result, code) {
      if (error || code !== 0) {
        grunt.log.warn("Chrome sandbox failed. Falling back to --no-sandbox mode.");
        grunt.log.warn("This may reduce security. Consider running in a proper environment with sandbox support.");
        
        grunt.task.run("qunit:withoutSandbox");
        done();
      } else {
        grunt.log.ok("Chrome ran successfully with sandbox enabled.");
        done();
      }
    });
  });

  grunt.registerTask("default", ["connect", "qunit-with-fallback"]);
};
