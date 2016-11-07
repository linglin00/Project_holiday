var app = angular.module("myApp", [])
app.controller('namesCtrl', function($scope) {
    $scope.names = [
        'Africa',
        'Antartica',
        'Asia',
        'Central America and Caribbean',
        'Europe',
        'Middle East',
        'North America',
        'South America'
    ];
});

app.controller('namesActivityCtrl', function($scope) {
    $scope.names = [
        'Hike',
        'Diving',
        'Beach',
        'Snorkel',
        'Culture',
        'Bike',
        'Countryside',
        'Yoga',
        'Wildlife'
    ];
});


//Data
var cities = [
              {
                  city : 'India',
                  desc : 'This is the best country in the world!',
                  lat : 23.200000,
                  long : 79.225487
              },
              {
                  city : 'New Delhi',
                  desc : 'The Heart of India!',
                  lat : 28.500000,
                  long : 77.250000
              },
              {
                  city : 'Mumbai',
                  desc : 'Bollywood city!',
                  lat : 19.000000,
                  long : 72.90000
              },
              {
                  city : 'Kolkata',
                  desc : 'Howrah Bridge!',
                  lat : 22.500000,
                  long : 88.400000
              },
              {
                  city : 'Chennai  ',
                  desc : 'Idli Sambar!',
                  lat : 13.000000,
                  long : 80.250000
              }
          ];

          //Angular App Module and Controller
           app.controller('MapCtrl', function ($scope) {

	     var mapOptions = {
                  zoom: 4,
                  center: new google.maps.LatLng(25,80),
                  mapTypeId: google.maps.MapTypeId.TERRAIN
              }

              $scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);
            
              $scope.markers = [];

              var infoWindow = new google.maps.InfoWindow();

              var createMarker = function (info){

                  var marker = new google.maps.Marker({
                      map: $scope.map,
                      position: new google.maps.LatLng(info.lat, info.long),
                      title: info.city
                  });
                  marker.content = '<div class="infoWindowContent">' + info.desc + '</div>';

                  google.maps.event.addListener(marker, 'click', function(){
                      infoWindow.setContent('<h2>' + marker.title + '</h2>' + marker.content);
                      infoWindow.open($scope.map, marker);
                  });

                  $scope.markers.push(marker);

              }

              for (i = 0; i < cities.length; i++){
                  createMarker(cities[i]);
              }

              $scope.openInfoWindow = function(e, selectedMarker){
                  e.preventDefault();
                  google.maps.event.trigger(selectedMarker, 'click');
              }

          });